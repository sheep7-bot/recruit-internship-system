package com.recruit.service;

import com.recruit.entity.Company;
import com.recruit.entity.Job;

import java.util.List;
import java.util.Map;

// 思考：岗位服务接口——"岗位模块对外提供的全部能力"（能力清单/合同）
// 思考：企业端（发布/编辑/下架）、学生端（搜索/详情）、管理端（审核/列表）都用它
// 思考：实现类 JobServiceImpl 里还有 Redis 搜索缓存、越权校验等细节，调用方无需关心
public interface JobService {

    // 思考：企业发布岗位（新岗位 status=0 待审核）
    void publish(Long companyUserId, Job job);

    // 思考：企业修改自己的岗位（带归属校验）
    void update(Long companyUserId, Job form);

    // 思考：企业下架岗位（status 置 2）
    void offline(Long companyUserId, Long jobId);

    // 思考：岗位搜索（学生端）——条件筛选 + 分页 + Redis 缓存，返回 {records, total}
    Map<String, Object> search(String keyword, String city, String type, Integer page, Integer size);

    // 思考：岗位详情（带企业名）
    Map<String, Object> detail(Long jobId);

    // 思考：企业查看自己发布的全部岗位
    List<Map<String, Object>> listByCompany(Long companyUserId);

    // 思考：管理端按状态查岗位（null = 全部）
    List<Map<String, Object>> listByStatus(Integer status);

    // 思考：管理员审核岗位（pass=true 上架，false 驳回）
    void audit(Long jobId, boolean pass);

    // 思考：查当前登录企业的档案（企业端头部显示公司名）
    Company getCompanyByUserId(Long companyUserId);
}