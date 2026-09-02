package com.recruit.service;

import java.util.Map;

// 思考：投递服务接口——"投递模块对外提供的全部能力"（能力清单/合同）
// 思考：学生端（投递/我的投递）、企业端（投递列表/更新状态）都用它
// 思考：实现类 ApplyServiceImpl 里的"跨公司数据隔离校验"（checkJobOwned）是核心安全设计
public interface ApplyService {

    // 思考：学生投递（岗位须在招 + 不能重复投）
    void apply(Long studentId, Long jobId);

    // 思考：学生查看自己的投递记录（分页，返回 {records, total}）
    Map<String, Object> listByStudent(Long studentId, Integer page, Integer size);

    // 思考：企业查看某岗位的投递列表（分页，带归属校验，返回 {records, total}）
    Map<String, Object> listByJob(Long companyUserId, Long jobId, Integer page, Integer size);

    // 思考：更新投递状态（已投递/被查看/面试邀约，带归属校验）
    void updateStatus(Long companyUserId, Long applyId, String status);
}