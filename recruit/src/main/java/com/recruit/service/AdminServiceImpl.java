package com.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruit.entity.*;
import com.recruit.mapper.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 思考：管理端服务——企业审核、岗位审核、用户管理、数据统计四大块
// 思考：@Service 标注这是业务层，Spring 启动时创建实例，供 AdminController 构造器注入
@Service
public class AdminServiceImpl implements AdminService {

    // 思考：五张表的数据访问对象——管理端要管到整个平台的每一类数据
    private final CompanyMapper companyMapper;
    private final JobMapper jobMapper;
    private final UserMapper userMapper;
    private final ApplyMapper applyMapper;
    private final ResumeMapper resumeMapper;

    // 思考：构造器注入：Spring 启动时把五个依赖一次性传进来
    public AdminServiceImpl(CompanyMapper companyMapper, JobMapper jobMapper, UserMapper userMapper,
                        ApplyMapper applyMapper, ResumeMapper resumeMapper) {
        this.companyMapper = companyMapper;
        this.jobMapper = jobMapper;
        this.userMapper = userMapper;
        this.applyMapper = applyMapper;
        this.resumeMapper = resumeMapper;
    }

    /**
     * 思考：企业列表（管理端"企业审核"页）
     * 思考：approved 参数：0=待审核 / 1=通过 / 2=驳回，不传=查全部
     */
    public List<Company> listCompanies(Integer approved) {
        // 思考：eq 第一个参数是布尔开关——approved 为 null 时不拼这个条件，一个方法同时支持"全部"和"按状态"
        // 思考：先按审核状态升序排（待审核的排最前面，管理员优先处理），再按创建时间倒序（新的在前）
        return companyMapper.selectList(new LambdaQueryWrapper<Company>()
                .eq(approved != null, Company::getApproved, approved)
                .orderByAsc(Company::getApproved)
                .orderByDesc(Company::getCreateTime));
    }

    /**
     * 思考：企业入驻审核：pass=true 通过(approved=1)，false 驳回(approved=2)
     * 思考：审核通过后企业才能登录（登录校验见 UserService.login）
     */
    public void auditCompany(Long companyId, boolean pass) {
        // 思考：先查企业档案，不存在直接报错（防前端传假 id）
        Company company = companyMapper.selectById(companyId);
        if (company == null) throw new RuntimeException("企业不存在");
        // 思考：按审核结果设置状态：三元表达式，true→1 通过，false→2 驳回
        company.setApproved(pass ? 1 : 2);
        // 思考：updateById 按主键更新，只更新非 null 字段
        companyMapper.updateById(company);
    }

    /**
     * 思考：用户列表（管理端"用户管理"页）
     */
    public List<User> listUsers() {
        // 思考：selectList(null)——wrapper 传 null 表示无条件，查整张表
        List<User> users = userMapper.selectList(null);
        // 思考：安全红线：返回给前端的用户对象必须抹掉密码，和登录接口同一原则
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    /**
     * 思考：启用/禁用账号：status=1 正常 / status=0 禁用（禁用后无法登录）
     */
    public void updateUserStatus(Long userId, Integer status) {
        // 思考：先查用户，不存在报错
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        // 思考：保护管理员账号：管理员不能被禁用，否则平台没人管了
        if ("admin".equals(user.getRole())) throw new RuntimeException("不能禁用管理员账号");
        // 思考：设置新状态并落库
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 思考：平台数据统计——各类数量汇总，前端用 ECharts 画仪表盘
     */
    public Map<String, Object> stats() {
        // 思考：HashMap 组装结果，key 是统计项名，value 是数量（key 名即前端 ECharts 的字段名）
        Map<String, Object> stats = new HashMap<>();
        // 思考：学生数量：t_user 表 role=student 的条数
        stats.put("studentCount", userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student")));
        // 思考：企业数量：企业档案表总数
        stats.put("companyCount", companyMapper.selectCount(null));
        // 思考：在招岗位数量：status=1（发布中）的岗位数
        stats.put("jobCount", jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, 1)));
        // 思考：投递总数：投递记录表条数
        stats.put("applyCount", applyMapper.selectCount(null));
        // 思考：简历总数
        stats.put("resumeCount", resumeMapper.selectCount(null));
        // 思考：待审核企业数：approved=0，管理员待办提醒用
        stats.put("pendingCompanyCount", companyMapper.selectCount(new LambdaQueryWrapper<Company>()
                .eq(Company::getApproved, 0)));
        // 思考：待审核岗位数：status=0
        stats.put("pendingJobCount", jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, 0)));
        return stats;
    }
}