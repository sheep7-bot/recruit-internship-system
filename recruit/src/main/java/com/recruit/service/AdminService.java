package com.recruit.service;

import com.recruit.entity.Company;
import com.recruit.entity.User;

import java.util.List;
import java.util.Map;

// 思考：管理端服务接口——"管理端对外提供的全部能力"（能力清单/合同）
// 思考：企业审核、岗位审核（岗位部分见 JobService）、用户管理、数据统计
public interface AdminService {

    // 思考：企业列表（approved 传 0 只看待审核，不传查全部）
    List<Company> listCompanies(Integer approved);

    // 思考：企业入驻审核（pass=true 通过，false 驳回）
    void auditCompany(Long companyId, boolean pass);

    // 思考：用户列表（已抹掉密码）
    List<User> listUsers();

    // 思考：启用/禁用账号（status=0 禁用，1 恢复）
    void updateUserStatus(Long userId, Integer status);

    // 思考：平台数据统计（前端 ECharts 用）
    Map<String, Object> stats();
}