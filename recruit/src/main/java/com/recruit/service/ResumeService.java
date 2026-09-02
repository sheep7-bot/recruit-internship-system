package com.recruit.service;

import com.recruit.entity.Resume;

// 思考：简历服务接口——"简历模块对外提供的全部能力"（能力清单/合同）
// 思考：调用方（Controller、AiResumeServiceImpl）只依赖本接口；实现类 ResumeServiceImpl 负责具体逻辑
public interface ResumeService {

    // 思考：按学生查简历，没有返回 null（前端据此显示"先填写简历"）
    Resume getByUserId(Long userId);

    // 思考：保存/更新简历——有则更新、无则新建
    void save(Long userId, Resume form);

    // 思考：按 id 查（AI 简历解析用）
    Resume getById(Long id);

    // 思考：AI 解析完成后回填结构化字段
    void update(Resume resume);
}