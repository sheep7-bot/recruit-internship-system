package com.recruit.service;

import com.recruit.entity.Resume;

// 思考：AI 简历解析服务接口——把简历原文交给大模型抽取成结构化字段（学历/技能/经历）
// 思考：实现类 AiResumeServiceImpl 里有提示词设计、JSON 清洗、原值保留等细节
public interface AiResumeService {

    // 思考：解析指定简历：LLM 抽取 → 回填数据库，返回更新后的简历
    Resume parse(Long resumeId);
}