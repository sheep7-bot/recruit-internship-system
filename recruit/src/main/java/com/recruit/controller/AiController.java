package com.recruit.controller;

import com.recruit.common.Result;
import com.recruit.entity.Resume;
import com.recruit.entity.User;
import com.recruit.service.AiMatchService;
import com.recruit.service.AiResumeService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

// 思考：AI 智能接口——问答 / 简历解析 / 智能岗位推荐 / 投递分析
// 思考：/api/ai/** 由拦截器保证只有 student 角色能访问
// 思考：chatClient 用的是 ChatClientConfig 里配好系统提示词的全局实例（AI 是刘亦菲人设）
@Tag(name = "05-AI 智能", description = "问答、简历解析、推荐、投递分析（角色：student）")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    // 思考：聊天客户端——AI 问答直接用它；简历解析/推荐/分析委托给两个 AiService
    private final ChatClient chatClient;
    private final AiResumeService aiResumeService;
    private final AiMatchService aiMatchService;
    // 思考：Redis 模板——AI 接口限流计数用（大模型按 token 计费，必须防刷）
    private final StringRedisTemplate redis;

    // 思考：限流参数：每个学生每分钟最多 10 次 AI 提问（超过返回友好提示）
    private static final String RATELIMIT_PREFIX = "ratelimit:ai:";
    private static final long RATELIMIT_MAX = 10;

    // 思考：构造器注入四个依赖
    public AiController(ChatClient chatClient, AiResumeService aiResumeService,
                        AiMatchService aiMatchService, StringRedisTemplate redis) {
        this.chatClient = chatClient;
        this.aiResumeService = aiResumeService;
        this.aiMatchService = aiMatchService;
        this.redis = redis;
    }

    // 思考：Redis 固定窗口限流：一分钟内累计提问次数，超过上限拒绝
    // 思考：INCR 计数 + 第一次设 60 秒过期，到点自动清零重新计数
    private boolean overLimit(Long userId) {
        // 思考：key 按用户区分：ratelimit:ai:{userId}，每次提问 INCR +1
        String key = RATELIMIT_PREFIX + userId;
        Long count = redis.opsForValue().increment(key);
        // 思考：count == 1 说明是这一分钟内的第一次提问，给 key 设 60 秒过期（固定窗口）
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofSeconds(60));
        }
        // 思考：超过 10 次返回 true（超限），由调用方拒绝请求
        return count != null && count > RATELIMIT_MAX;
    }

    @Operation(summary = "AI 问答（一次性）", description = "流式不可用时的降级接口")
    // 思考：智能问答：POST /api/ai/chat  body: {"question":"实习面试要注意什么"}
    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestAttribute User loginUser, @RequestBody Map<String, String> req) {
        // 思考：取问题，为空直接返回业务错误（Result.error 走统一 JSON）
        String question = req.getOrDefault("question", "");
        if (question.isBlank()) {
            return Result.error("问题不能为空");
        }
        // 思考：限流检查：超限返回友好提示，不调大模型
        if (overLimit(loginUser.getId())) {
            return Result.error("提问太频繁啦，请一分钟后再试");
        }
        try {
            // 思考：调大模型：prompt(question) → call() 同步调用 → content() 拿回答
            String answer = chatClient.prompt(question).call().content();
            // 思考：Map.of 装返回值，前端取 data.answer 显示
            return Result.ok(Map.of("answer", answer));
        } catch (Exception e) {
            // 思考：AI 降级：大模型服务不可用时返回友好提示，接口不报错
            return Result.ok(Map.of("answer", "AI 服务暂时不可用，请稍后再试"));
        }
    }

    @Operation(summary = "AI 问答（SSE 流式）", description = "逐段推送，前端打字机渲染；每人每分钟限 10 次")
    // 思考：智能问答（流式/打字机效果）：POST /api/ai/chat/stream
    // 思考：返回 SSE（text/event-stream）：大模型每生成一小段文字就推给前端一段，
    // 思考：前端边收边渲染，就是常见的"AI 一个字一个字往外蹦"的效果
    // 思考：Spring MVC 原生支持 Flux 返回值：每个元素会被包装成一条 SSE data 事件
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestAttribute User loginUser, @RequestBody Map<String, String> req) {
        // 思考：流式接口不能直接 return Result 包错误（类型不匹配），用 Flux.just 发一段错误文本
        String question = req.getOrDefault("question", "");
        if (question.isBlank()) {
            return Flux.just("问题不能为空");
        }
        // 思考：限流同样适用流式接口
        if (overLimit(loginUser.getId())) {
            return Flux.just("提问太频繁啦，请一分钟后再试");
        }
        // 思考：stream().content() 返回 Flux——大模型边生成边推送
        return chatClient.prompt(question).stream().content()
                // 思考：AI 降级：流式中途出错也要给前端一个收尾提示，不能让页面一直转圈
                .onErrorResume(e -> Flux.just("\n\n【AI 服务暂时不可用，请稍后再试】"));
    }

    @Operation(summary = "AI 简历解析", description = "抽取姓名/学校/学历/年龄/技能/经历回填")
    // 思考：AI 简历解析：POST /api/ai/parse-resume/1
    // 思考：大模型从简历原文抽取 学历/技能/经历 等结构化字段并回填数据库
    @PostMapping("/parse-resume/{resumeId}")
    public Result<Resume> parseResume(@PathVariable Long resumeId) {
        // 思考：委托 AiResumeService：解析成功返回回填后的简历对象
        return Result.ok(aiResumeService.parse(resumeId));
    }

    @Operation(summary = "智能岗位推荐", description = "按学生简历挑最匹配的 3 个岗位")
    // 思考：智能岗位推荐：GET /api/ai/recommend（大模型按简历挑最匹配的 3 个岗位）
    @GetMapping("/recommend")
    public Result<List<Map<String, Object>>> recommend(@RequestAttribute User loginUser) {
        // 思考：loginUser.getId() 是学生 id，AiMatchService 查它的简历做推荐
        return Result.ok(aiMatchService.recommend(loginUser.getId()));
    }

    @Operation(summary = "AI 投递分析", description = "匹配度评估 + 亮点 + 改进建议")
    // 思考：AI 投递分析：GET /api/ai/apply-advice/{applyId}
    // 思考：学生查看自己某次投递：大模型分析简历与该岗位的匹配度，并给出优化建议
    @GetMapping("/apply-advice/{applyId}")
    public Result<String> applyAdvice(@RequestAttribute User loginUser, @PathVariable Long applyId) {
        // 思考：Service 内部校验"只能分析自己的投递"，返回分析文本直接给前端渲染
        return Result.ok(aiMatchService.applyAdvice(loginUser.getId(), applyId));
    }
}