package com.recruit.service;

import java.util.List;
import java.util.Map;

// 思考：AI 匹配服务接口——项目核心亮点的"双通道 AI 筛选"能力清单
// 思考：通道一 规则筛选（硬性条件）→ 通道二 LLM 评分（软性匹配），全部细节在实现类 AiMatchServiceImpl
// 思考：企业端（AI 筛选候选人）、学生端（智能推荐、投递分析）共用本接口
public interface AiMatchService {

    // 思考：对某岗位全部投递做一轮 AI 筛选，返回带评分的结果（按分数降序）
    List<Map<String, Object>> filter(Long companyUserId, Long jobId);

    // 思考：智能岗位推荐——按学生简历挑最匹配的 3 个岗位
    List<Map<String, Object>> recommend(Long studentId);

    // 思考：AI 投递分析——匹配度评估 + 亮点 + 改进建议
    String applyAdvice(Long studentId, Long applyId);
}