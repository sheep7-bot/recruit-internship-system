package com.recruit.controller;

import com.recruit.common.Result;
import com.recruit.entity.Company;
import com.recruit.entity.Job;
import com.recruit.entity.User;
import com.recruit.service.AiMatchService;
import com.recruit.service.ApplyService;
import com.recruit.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 思考：企业（HR）端接口——岗位管理、候选人筛选、AI 筛选
// 思考：/api/company/** 由拦截器保证只有 company 角色能访问（入驻审核在登录时已校验）
@Tag(name = "03-企业端", description = "岗位管理、候选人筛选、AI 筛选（角色：company）")
@RestController
@RequestMapping("/api/company")
public class CompanyController {

    // 思考：岗位管理依赖 JobService、候选人筛选依赖 ApplyService、AI 筛选依赖 AiMatchService
    private final JobService jobService;
    private final ApplyService applyService;
    private final AiMatchService aiMatchService;

    // 思考：构造器注入三个 Service
    public CompanyController(JobService jobService, ApplyService applyService, AiMatchService aiMatchService) {
        this.jobService = jobService;
        this.applyService = applyService;
        this.aiMatchService = aiMatchService;
    }

    @Operation(summary = "我的企业信息", description = "企业端头部展示公司名称用")
    // 思考：当前登录企业的信息：GET /api/company/info（企业端头部展示公司名称用）
    @GetMapping("/info")
    public Result<Company> info(@RequestAttribute User loginUser) {
        // 思考：按登录用户查企业档案（getCompanyByUserId 在 JobService 里，复用）
        return Result.ok(jobService.getCompanyByUserId(loginUser.getId()));
    }

    @Operation(summary = "我发布的岗位", description = "只含本公司的岗位")
    // 思考：我发布的全部岗位（含各审核状态）：GET /api/company/jobs
    @GetMapping("/jobs")
    public Result<List<Map<String, Object>>> myJobs(@RequestAttribute User loginUser) {
        // 思考：listByCompany 按登录用户定位企业，只查自己公司的岗位
        return Result.ok(jobService.listByCompany(loginUser.getId()));
    }

    @Operation(summary = "发布岗位", description = "新岗位为待审核状态，管理员通过后上架")
    // 思考：发布岗位：POST /api/company/jobs（发布后进入"待审核"状态，管理员通过才上架）
    @PostMapping("/jobs")
    public Result<?> publish(@RequestAttribute User loginUser, @RequestBody Job job) {
        // 思考：companyId 由服务端登录用户推导，企业发不出"挂别人公司名下"的岗位
        jobService.publish(loginUser.getId(), job);
        return Result.ok("发布成功，等待管理员审核");
    }

    @Operation(summary = "修改岗位", description = "只能改本公司的岗位")
    // 思考：修改岗位：PUT /api/company/jobs
    @PutMapping("/jobs")
    public Result<?> update(@RequestAttribute User loginUser, @RequestBody Job job) {
        // 思考：getOwned 校验岗位归属，改别人的岗位会被拒绝
        jobService.update(loginUser.getId(), job);
        return Result.ok("修改成功");
    }

    @Operation(summary = "下架岗位")
    // 思考：下架岗位：PUT /api/company/jobs/1/offline
    @PutMapping("/jobs/{id}/offline")
    public Result<?> offline(@RequestAttribute User loginUser, @PathVariable Long id) {
        // 思考：同样先做归属校验再下架（status 置 2，学生端立即可见不到）
        jobService.offline(loginUser.getId(), id);
        return Result.ok("已下架");
    }

    @Operation(summary = "查看投递列表", description = "带学生简历和 AI 评分；跨公司查询会被拒绝；支持分页")
    // 思考：查看某岗位的投递列表（带学生简历和 AI 评分）：GET /api/company/applies?jobId=1&page=1&size=10
    // 思考：loginUser 传进 service 做岗位归属校验，防止跨公司查投递（水平越权防护）
    @GetMapping("/applies")
    public Result<Map<String, Object>> applies(@RequestAttribute User loginUser, @RequestParam Long jobId,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return Result.ok(applyService.listByJob(loginUser.getId(), jobId, page, size));
    }

    @Operation(summary = "更新投递状态", description = "已投递/被查看/面试邀约")
    // 思考：更新投递状态：PUT /api/company/applies/1/status  body: {"status":"面试邀约"}
    @PutMapping("/applies/{id}/status")
    public Result<?> updateApplyStatus(@RequestAttribute User loginUser, @PathVariable Long id,
                                       @RequestBody Map<String, String> req) {
        // 思考：Service 内部校验状态值白名单 + 岗位归属，防止传奇怪的值/改别人的投递
        applyService.updateStatus(loginUser.getId(), id, req.get("status"));
        return Result.ok("状态已更新");
    }

    @Operation(summary = "AI 筛选候选人", description = "规则过滤 + 大模型评分，按分排序")
    // 思考：AI 筛选候选人：POST /api/company/ai-filter/1
    // 思考：双通道：规则过滤 → LLM 评分，返回按匹配分排序的候选人列表（项目核心亮点）
    @PostMapping("/ai-filter/{jobId}")
    public Result<List<Map<String, Object>>> aiFilter(@RequestAttribute User loginUser, @PathVariable Long jobId) {
        // 思考：loginUser.getId() 是公司账号 id，AiMatchService 用它校验岗位归属
        return Result.ok(aiMatchService.filter(loginUser.getId(), jobId));
    }
}