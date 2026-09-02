package com.recruit.controller;

import com.recruit.common.Result;
import com.recruit.entity.Company;
import com.recruit.entity.User;
import com.recruit.service.AdminService;
import com.recruit.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 思考：管理端接口——企业/岗位审核、用户管理、数据统计
// 思考：/api/admin/** 由拦截器保证只有 admin 角色能访问（admin 账号由初始化脚本写入数据库）
@Tag(name = "04-管理端", description = "企业/岗位审核、用户管理、统计（角色：admin）")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 思考：管理业务在 AdminService（审核/用户/统计），岗位查询复用 JobService
    private final AdminService adminService;
    private final JobService jobService;

    // 思考：构造器注入两个 Service
    public AdminController(AdminService adminService, JobService jobService) {
        this.adminService = adminService;
        this.jobService = jobService;
    }

    @Operation(summary = "企业列表", description = "approved=0 只看待审核")
    // 思考：企业审核列表：GET /api/admin/companies?approved=0（0=待审核，不传=全部）
    @GetMapping("/companies")
    public Result<List<Company>> companies(@RequestParam(required = false) Integer approved) {
        // 思考：approved 为空时 Service 不拼条件，返回全部企业
        return Result.ok(adminService.listCompanies(approved));
    }

    @Operation(summary = "企业入驻审核")
    // 思考：企业入驻审核：PUT /api/admin/companies/1/audit  body: {"pass":true}
    @PutMapping("/companies/{id}/audit")
    public Result<?> auditCompany(@PathVariable Long id, @RequestBody Map<String, Boolean> req) {
        // 思考：Boolean.TRUE.equals(req.get("pass"))——req.get 可能返回 null，这样写防空指针
        // 思考：true=通过(approved=1)，false=驳回(approved=2)，通过后企业才能登录
        adminService.auditCompany(id, Boolean.TRUE.equals(req.get("pass")));
        return Result.ok("审核完成");
    }

    @Operation(summary = "岗位列表", description = "status=0 只看待审核")
    // 思考：岗位审核列表：GET /api/admin/jobs?status=0（0=待审核，不传=全部）
    @GetMapping("/jobs")
    public Result<List<Map<String, Object>>> jobs(@RequestParam(required = false) Integer status) {
        // 思考：listByStatus 复用 JobService（企业端和管理端都看岗位，只是筛选条件不同）
        return Result.ok(jobService.listByStatus(status));
    }

    @Operation(summary = "岗位审核")
    // 思考：岗位审核：PUT /api/admin/jobs/1/audit  body: {"pass":true}
    @PutMapping("/jobs/{id}/audit")
    public Result<?> auditJob(@PathVariable Long id, @RequestBody Map<String, Boolean> req) {
        // 思考：true=上架(status=1，学生端立即可见)，false=驳回；审核后自动清岗位搜索缓存
        jobService.audit(id, Boolean.TRUE.equals(req.get("pass")));
        return Result.ok("审核完成");
    }

    @Operation(summary = "用户列表", description = "已抹掉密码字段")
    // 思考：用户管理列表：GET /api/admin/users（Service 内部已抹掉密码）
    @GetMapping("/users")
    public Result<List<User>> users() {
        return Result.ok(adminService.listUsers());
    }

    @Operation(summary = "启用/禁用账号", description = "禁用后该用户无法登录")
    // 思考：启用/禁用账号：PUT /api/admin/users/2/status  body: {"status":0}
    // 思考：status=0 禁用（登录时被拦，见 UserService.login），status=1 恢复
    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> req) {
        // 思考：Service 内部保护：管理员账号不能被禁用
        adminService.updateUserStatus(id, req.get("status"));
        return Result.ok("操作成功");
    }

    @Operation(summary = "平台数据统计", description = "各实体数量汇总，前端 ECharts 使用")
    // 思考：平台数据统计：GET /api/admin/stats（学生/企业/岗位/投递等数量，前端 ECharts 画仪表盘）
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminService.stats());
    }
}