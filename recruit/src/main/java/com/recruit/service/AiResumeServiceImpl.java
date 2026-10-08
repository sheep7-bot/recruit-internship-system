package com.recruit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.entity.Resume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

// 思考：AI 简历解析服务——把学生粘贴的简历原文交给大模型，抽取成结构化字段（学历/技能/经历）回填简历表
// 思考：为什么解析？AI 筛选时直接用结构化字段做规则过滤，不用每次都解析全文
// 思考：@Service 标注这是业务层，Spring 启动时创建实例，供 AiController 注入
@Service
public class AiResumeServiceImpl implements AiResumeService {

    // 思考：日志器——解析失败时打印真实异常，方便排查（不能只吞异常留一句提示）
    private static final Logger log = LoggerFactory.getLogger(AiResumeServiceImpl.class);

    // 思考：字段长度上限 = t_resume 表对应列的长度（varchar 长度）
    // 思考：大模型经常不遵守"200字以内"，抽出来的经历可能几百上千字，
    // 思考：直接写库会触发 Data too long 报错（这正是"AI 解析暂时不可用"的历史根因），所以回填前统一截断
    private static final int MAX_NAME = 50;
    private static final int MAX_SCHOOL = 100;
    private static final int MAX_EDU = 50;
    private static final int MAX_SKILLS = 200;
    private static final int MAX_EXPERIENCE = 500;

    // 思考：SpringAI 的聊天客户端——发提示词调大模型就靠它（全局实例带默认人设，见 ChatClientConfig）
    private final ChatClient chatClient;
    // 思考：简历服务——查简历、保存解析结果都复用它
    private final ResumeService resumeService;
    // 思考：Jackson 的 JSON 解析器——把大模型返回的 JSON 字符串转成 JsonNode 取字段
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 思考：构造器注入两个依赖
    public AiResumeServiceImpl(ChatClient chatClient, ResumeService resumeService) {
        this.chatClient = chatClient;
        this.resumeService = resumeService;
    }

    /**
     * 思考：解析指定简历：查简历 → 组提示词 → 调 LLM → 解析 JSON → 回填数据库
     */
    public Resume parse(Long resumeId) {
        // 思考：先按 id 查简历，不存在报错
        Resume resume = resumeService.getById(resumeId);
        if (resume == null) throw new RuntimeException("简历不存在");
        // 思考：简历原文为空就没东西可解析，提示学生先填写
        if (resume.getContent() == null || resume.getContent().isBlank()) {
            throw new RuntimeException("简历内容为空，请先填写简历原文");
        }

        // 思考：提示词设计要点（大模型输出稳定性的关键）：
        // 思考：1. 明确要求"只输出 JSON，不要输出其他内容"——模型才不会被带偏
        // 思考：2. 给出目标格式示例——模型照着格式填，解析才不会报错
        // 思考：3. 要求缺失字段填空字符串/0——防止模型编造学生没写的信息
        String prompt = """
                请从下面的简历原文中抽取结构化信息，只输出一个 JSON 对象，不要输出任何其他内容。
                格式：{"name":"姓名","school":"学校","edu":"学历，如 本科/硕士/大专","age":整数年龄,"skills":"技能，逗号分隔","experience":"主要经历，200字以内"}
                要求：只使用原文中出现的信息，没有的字段填空字符串（年龄填 0）。

                简历原文：
                %s
                """.formatted(resume.getContent());

        try {
            // 思考：调大模型：prompt 组好 → call() 同步调用 → content() 拿回答文本
            String answer = chatClient.prompt(prompt).call().content();
            // 思考：cleanJson 去掉模型可能带的 ```json 包裹，再转 JsonNode
            JsonNode node = objectMapper.readTree(cleanJson(answer));
            // 思考：逐个字段回填——textOrKeep/intOrKeep：模型没抽出来就保留原值，不覆盖学生手填内容
            // 思考：cut() 按表列长截断，防止超长字段写库报 Data too long
            resume.setName(cut(textOrKeep(node.get("name"), resume.getName()), MAX_NAME));
            resume.setSchool(cut(textOrKeep(node.get("school"), resume.getSchool()), MAX_SCHOOL));
            resume.setEdu(cut(textOrKeep(node.get("edu"), resume.getEdu()), MAX_EDU));
            resume.setAge(intOrKeep(node.get("age"), resume.getAge()));
            resume.setSkills(cut(textOrKeep(node.get("skills"), resume.getSkills()), MAX_SKILLS));
            resume.setExperience(cut(textOrKeep(node.get("experience"), resume.getExperience()), MAX_EXPERIENCE));
            // 思考：更新数据库并返回更新后的简历给前端展示
            resumeService.update(resume);
            return resume;
        } catch (Exception e) {
            // 思考：打印真实异常（HTTP 状态码/JSON 解析/数据库超长），不要再把原因吞掉
            log.error("AI 简历解析失败, resumeId={}", resumeId, e);
            // 思考：AI 降级：解析失败不影响主流程，提示学生手动填写结构化字段
            throw new RuntimeException("AI 解析暂时不可用，请手动填写学历/技能/经历字段");
        }
    }

    // 思考：截断工具——超过 max 就直接砍掉多余部分，保证能写进 varchar 列
    private String cut(String v, int max) {
        return v != null && v.length() > max ? v.substring(0, max) : v;
    }

    // 思考：清理工具——去掉大模型可能带的 ```json 代码块包裹，只留 JSON 本体
    private String cleanJson(String answer) {
        // 思考：先去掉首尾空白
        String text = answer.trim();
        // 思考：以 ``` 开头说明模型带了 Markdown 代码块，剥掉开头和结尾的反引号
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }
        // 思考：兜底：直接从第一个 { 截到最后一个 }——万一模型前后还啰嗦了几句也能救回来
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text;
    }

    // 思考：字段取值工具——模型没抽出来（null/空串）就返回原值，保证学生手填的旧数据不被冲掉
    private String textOrKeep(JsonNode node, String fallback) {
        // 思考：节点不存在或 JSON null → 保留原值
        if (node == null || node.isNull()) return fallback;
        // 思考：抽出来但去空格后是空串 → 也保留原值
        String v = node.asText().trim();
        return v.isEmpty() ? fallback : v;
    }

    // 思考：整数版同上：年龄没抽出有效值（缺失/0/负数）就保留原值
    private Integer intOrKeep(JsonNode node, Integer fallback) {
        // 思考：节点不存在或 JSON null → 保留原值
        if (node == null || node.isNull()) return fallback;
        // 思考：asInt(0)——不是数字时取默认 0，再判断是否大于 0，无效就保留原值
        int v = node.asInt(0);
        return v > 0 ? v : fallback;
    }
}