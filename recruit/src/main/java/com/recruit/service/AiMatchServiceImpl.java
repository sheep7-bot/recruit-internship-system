package com.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.entity.AiScore;
import com.recruit.entity.Apply;
import com.recruit.entity.Job;
import com.recruit.entity.Resume;
import com.recruit.mapper.AiScoreMapper;
import com.recruit.mapper.ApplyMapper;
import com.recruit.mapper.JobMapper;
import com.recruit.mapper.ResumeMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 思考：AI 智能筛选服务——项目核心亮点，双通道设计（对应需求文档的时序图）
// 思考：通道一 规则筛选（硬性条件）：学历/技能关键词不满足的直接标记"不符合规则"，不浪费大模型调用——快、免费、可解释
// 思考：通道二 LLM 评分（软性匹配）：通过规则的候选人交给大模型按岗位要求打 0-100 分并给理由——准、有语义理解
// 思考：@Service 标注这是业务层，Spring 启动时创建实例，供 CompanyController / AiController 注入
@Service
public class AiMatchServiceImpl implements AiMatchService {

    // 思考：SpringAI 聊天客户端——发提示词调大模型（全局实例，见 ChatClientConfig）
    private final ChatClient chatClient;
    // 思考：四张表的数据访问对象 + 企业表——筛选要拼岗位、投递、简历、评分多表数据
    private final JobMapper jobMapper;
    private final ApplyMapper applyMapper;
    private final ResumeMapper resumeMapper;
    private final AiScoreMapper aiScoreMapper;
    private final com.recruit.mapper.CompanyMapper companyMapper;
    // 思考：Jackson JSON 解析器——解析大模型返回的 JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 思考：构造器注入全部依赖
    public AiMatchServiceImpl(ChatClient chatClient, JobMapper jobMapper, ApplyMapper applyMapper,
                          ResumeMapper resumeMapper, AiScoreMapper aiScoreMapper,
                          com.recruit.mapper.CompanyMapper companyMapper) {
        this.chatClient = chatClient;
        this.jobMapper = jobMapper;
        this.applyMapper = applyMapper;
        this.resumeMapper = resumeMapper;
        this.aiScoreMapper = aiScoreMapper;
        this.companyMapper = companyMapper;
    }

    /**
     * 思考：对某岗位的全部投递做一轮 AI 筛选，返回带评分的结果列表（按分数从高到低）
     * 思考：companyUserId 用来校验岗位归属，防止企业查到别家公司的投递数据（水平越权防护）
     */
    public List<Map<String, Object>> filter(Long companyUserId, Long jobId) {
        // 思考：第一步：查岗位，不存在报错
        Job job = jobMapper.selectById(jobId);
        if (job == null) throw new RuntimeException("岗位不存在");
        // 思考：第二步：按登录用户查企业档案，拿 companyId
        com.recruit.entity.Company company = companyMapper.selectOne(
                new LambdaQueryWrapper<com.recruit.entity.Company>()
                        .eq(com.recruit.entity.Company::getUserId, companyUserId));
        // 思考：第三步：岗位归属校验——岗位必须属于当前企业，否则拒绝（防跨公司看数据）
        if (company == null || !job.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("该岗位不属于当前企业，无权操作");
        }
        // 思考：第四步：查这个岗位的全部投递记录
        List<Apply> applies = applyMapper.selectList(new LambdaQueryWrapper<Apply>()
                .eq(Apply::getJobId, jobId));

        // 思考：第五步：逐条投递处理——先规则筛选，过了再 LLM 评分
        List<Map<String, Object>> result = new ArrayList<>();
        for (Apply apply : applies) {
            // 思考：每个候选人组装一个 Map：投递 id + 投递状态打底
            Map<String, Object> item = new HashMap<>();
            item.put("applyId", apply.getId());
            item.put("status", apply.getStatus());

            // 思考：查候选人的简历（每个学生最新一份）
            Resume resume = getResume(apply.getStudentId());
            if (resume == null) {
                // 思考：没简历没法筛：标记未过规则 + 0 分 + 原因，直接进结果列表
                item.put("studentName", "未知学生");
                item.put("rulePassed", false);
                item.put("aiScore", 0);
                item.put("aiReason", "学生尚未填写简历");
                result.add(item);
                continue;
            }
            item.put("studentName", resume.getName());

            // ── 通道一：规则筛选（硬性条件，免费快速）──
            // 思考：checkRule 返回 null=通过；返回字符串=不通过的原因
            String ruleFail = checkRule(job, resume);
            if (ruleFail != null) {
                // 思考：没过规则：0 分 + 原因，不再调大模型（省 token）
                item.put("rulePassed", false);
                item.put("aiScore", 0);
                item.put("aiReason", "未通过规则筛选：" + ruleFail);
                // 思考：规则拒绝的结果也要落库（0分+原因）——HR 刷新页面后仍能看到筛选结论
                saveScore(apply.getId(), 0, "未通过规则筛选：" + ruleFail);
                result.add(item);
                continue;
            }

            // ── 通道二：LLM 评分（软性匹配，花 token 的部分）──
            int score;
            String reason;
            try {
                // 思考：组装评分提示词：岗位要求 + 候选人简历摘要，要求只输出 JSON
                String prompt = """
                        你是招聘助手。请根据岗位要求给候选人简历打一个 0-100 的匹配分，并说明理由。
                        只输出一个 JSON 对象，格式：{"score":整数评分,"reason":"50字以内的评分理由"}

                        岗位：%s，城市：%s，技能要求：%s，学历要求：%s
                        岗位描述：%s

                        候选人简历：
                        %s
                        """.formatted(job.getTitle(), job.getCity(), job.getSkills(),
                        job.getEduReq(), job.getDescription(), resumeBrief(resume));
                // 思考：调大模型拿回答 → 清理 → 解析 JSON
                String answer = chatClient.prompt(prompt).call().content();
                JsonNode node = objectMapper.readTree(cleanJson(answer));
                // 思考：取分：缺失默认 50，clamp 限制在 0-100
                score = clamp(node.get("score") == null ? 50 : node.get("score").asInt(50));
                reason = node.get("reason") == null ? "" : node.get("reason").asText();
            } catch (Exception e) {
                // 思考：AI 降级：大模型不可用时给中性分 50，不阻断筛选流程
                score = 50;
                reason = "AI 评分服务暂时不可用，请人工判断";
            }

            // 思考：评分落库（先删旧的再插新的=覆盖，防止重复筛选产生多条记录）
            saveScore(apply.getId(), score, reason);

            // 思考：组装进结果：过了规则 + 分数 + 理由
            item.put("rulePassed", true);
            item.put("aiScore", score);
            item.put("aiReason", reason);
            result.add(item);
        }

        // 思考：按分数从高到低排序，HR 从最匹配的候选人看起
        result.sort((a, b) -> Integer.compare((int) b.get("aiScore"), (int) a.get("aiScore")));
        return result;
    }

    // 思考：保存/覆盖某条投递的 AI 评分——先按 applyId 删旧记录，再插入新的（保证一条投递只有一条最新评分）
    private void saveScore(Long applyId, int score, String reason) {
        aiScoreMapper.delete(new LambdaQueryWrapper<AiScore>()
                .eq(AiScore::getApplyId, applyId));
        AiScore aiScore = new AiScore();
        aiScore.setApplyId(applyId);
        aiScore.setScore(score);
        aiScore.setReason(reason);
        aiScoreMapper.insert(aiScore);
    }

    /**
     * 思考：智能岗位推荐（学生端）——拿学生简历和全部在招岗位，让大模型挑出最匹配的 3 个
     */
    public List<Map<String, Object>> recommend(Long studentId) {
        // 思考：先查学生简历，没有简历没法推荐
        Resume resume = getResume(studentId);
        if (resume == null) throw new RuntimeException("请先填写简历再使用智能推荐");

        // 思考：查全部在招岗位（status=1），limit 20 防止岗位太多塞爆提示词
        List<Job> jobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, 1)
                .last("limit 20"));
        // 思考：没有在招岗位直接返回空列表
        if (jobs.isEmpty()) return List.of();

        // 思考：把岗位压缩成 "id:标题:技能要求" 的清单——不传完整字段，节省 token
        StringBuilder catalog = new StringBuilder();
        for (Job job : jobs) {
            catalog.append(job.getId()).append(":").append(job.getTitle())
                    .append(":").append(job.getSkills()).append("\n");
        }

        // 思考：结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 思考：组装推荐提示词：要求只输出 JSON 数组，格式带示例
            String prompt = """
                    你是求职助手。根据学生简历，从下面的岗位清单中挑出最匹配的 3 个岗位。
                    只输出一个 JSON 数组，格式：[{"id":岗位id,"reason":"30字以内推荐理由"}]

                    学生简历：%s
                    岗位清单（id:标题:技能要求）：
                    %s
                    """.formatted(resumeBrief(resume), catalog);
            // 思考：调大模型 → 清理 → 解析成 JSON 数组
            String answer = chatClient.prompt(prompt).call().content();
            JsonNode array = objectMapper.readTree(cleanJsonArray(answer));
            // 思考：遍历数组，按 id 找回完整岗位信息组装结果
            for (JsonNode node : array) {
                long id = node.get("id").asLong();
                // 思考：过滤掉模型编造的 id（不存在的岗位）——findFirst 匹配到了才组装
                jobs.stream().filter(j -> j.getId().equals(id)).findFirst().ifPresent(job -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", job.getId());
                    item.put("title", job.getTitle());
                    item.put("city", job.getCity());
                    item.put("salary", job.getSalary());
                    item.put("type", job.getType());
                    item.put("skills", job.getSkills());
                    item.put("eduReq", job.getEduReq());
                    item.put("reason", node.get("reason") == null ? "" : node.get("reason").asText());
                    result.add(item);
                });
            }
        } catch (Exception e) {
            // 思考：AI 降级：推荐失败就退回"最新 3 个岗位"，主流程不报错
            for (Job job : jobs.stream().limit(3).toList()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", job.getId());
                item.put("title", job.getTitle());
                item.put("city", job.getCity());
                item.put("salary", job.getSalary());
                item.put("type", job.getType());
                item.put("skills", job.getSkills());
                item.put("eduReq", job.getEduReq());
                item.put("reason", "AI 推荐暂时不可用，为你推荐最新岗位");
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 思考：AI 投递分析（学生端）——大模型分析"这份简历投这个岗位"的匹配度和优化建议
     */
    public String applyAdvice(Long studentId, Long applyId) {
        // 思考：先查投递记录，并校验只能分析自己的投递（防越权看别人的）
        Apply apply = applyMapper.selectById(applyId);
        if (apply == null || !apply.getStudentId().equals(studentId)) {
            throw new RuntimeException("投递记录不存在");
        }
        // 思考：查岗位和简历，任一缺失无法分析
        Job job = jobMapper.selectById(apply.getJobId());
        Resume resume = getResume(studentId);
        if (job == null || resume == null) {
            throw new RuntimeException("岗位或简历信息缺失，无法分析");
        }
        // 思考：组装分析提示词：要求按三段结构输出，控制总长 300 字
        String prompt = """
                你是求职辅导老师。一位学生把简历投递给了下面的岗位，请给出投递分析，帮助他提高竞争力。
                按以下结构输出（总长 300 字以内，用中文）：
                1. 匹配度评估：一句话结论（高/中/低）+ 理由
                2. 简历亮点：1-2 条与该岗位相关的优势
                3. 改进建议：2-3 条针对这个岗位的具体优化动作

                岗位：%s（%s，%s）技能要求：%s，学历要求：%s
                岗位描述：%s

                学生简历：%s
                """.formatted(job.getTitle(), job.getCity(), job.getSalary(), job.getSkills(),
                job.getEduReq(), job.getDescription(), resumeBrief(resume));
        try {
            // 思考：调大模型直接返回文本（这个接口不需要 JSON，直接给前端展示）
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            // 思考：AI 降级：分析不可用给友好提示
            throw new RuntimeException("AI 分析暂时不可用，请稍后再试");
        }
    }

    // 思考：查学生的简历——每个学生取最新一份（orderByDesc(id) + limit 1）
    private Resume getResume(Long studentId) {
        return resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, studentId)
                .orderByDesc(Resume::getId)
                .last("limit 1"));
    }

    // ── 通道一：规则筛选的具体规则 ──
    // 思考：返回 null 表示通过；返回字符串表示不通过的原因（附在 AI 评分理由里给 HR 看）
    private String checkRule(Job job, Resume resume) {
        // 思考：规则1 学历要求：简历学历里要包含岗位要求的关键词（如"本科"），否则不通过
        if (job.getEduReq() != null && !job.getEduReq().isBlank()
                && resume.getEdu() != null
                && !resume.getEdu().contains(job.getEduReq())) {
            return "学历要求 " + job.getEduReq();
        }
        // 思考：规则2 技能要求：岗位要求的技能至少命中一项（两边都按逗号/顿号分隔成集合比较）
        if (job.getSkills() != null && !job.getSkills().isBlank()) {
            // 思考：岗位要求的技能列表（兼容中文逗号、顿号分隔）
            List<String> required = Arrays.asList(job.getSkills().split("[,，、]"));
            // 思考：简历里的技能列表，简历没填就是空列表
            List<String> owned = resume.getSkills() == null
                    ? List.of() : Arrays.asList(resume.getSkills().split("[,，、]"));
            // 思考：anyMatch 双重循环：岗位任一技能 命中 简历任一技能 即算通过（忽略大小写）
            boolean hit = required.stream().anyMatch(r ->
                    owned.stream().anyMatch(o -> !o.isBlank() && r.trim().toLowerCase().contains(o.trim().toLowerCase())));
            if (!hit) {
                return "技能要求 " + job.getSkills();
            }
        }
        // 思考：全部规则通过
        return null;
    }

    // 思考：简历摘要——结构化字段 + 全文前 500 字，控制传给大模型的长度（省 token 也是省响应时间）
    private String resumeBrief(Resume resume) {
        // 思考：简历原文可能很长，截前 500 字
        String content = resume.getContent() == null ? "" : resume.getContent();
        if (content.length() > 500) content = content.substring(0, 500);
        // 思考：拼成一行文本给大模型：学历/技能/经历/原文
        return "学历:" + safe(resume.getEdu()) + "；技能:" + safe(resume.getSkills())
                + "；经历:" + safe(resume.getExperience()) + "；简历原文:" + content;
    }

    // 思考：空值兜底——null 转空字符串，防止拼提示词时报 NPE
    private String safe(String s) {
        return s == null ? "" : s;
    }

    // 思考：评分限制在 0-100——大模型偶尔抽风输出 200 分，clamp 兜底
    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    // 思考：清理工具——去掉 ```json 包裹（对象版，与 AiResumeService 相同处理）
    private String cleanJson(String answer) {
        String text = answer.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) text = text.substring(start, end + 1);
        return text;
    }

    // 思考：清理工具——数组版：从第一个 [ 截到最后一个 ]（推荐接口返回 JSON 数组用）
    private String cleanJsonArray(String answer) {
        String text = answer.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) text = text.substring(start, end + 1);
        return text;
    }
}