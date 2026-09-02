package com.recruit.controller;

import com.recruit.common.Result;
import com.recruit.entity.Resume;
import com.recruit.entity.User;
import com.recruit.service.ApplyService;
import com.recruit.service.JobService;
import com.recruit.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 思考：学生端接口——求职者的全部功能（岗位搜索、投递、简历）都在这个类里
// 思考：/api/student/** 由拦截器保证只有 student 角色能访问（见 AuthInterceptor）
// 思考：loginUser 是拦截器校验通过后挂在 request 上的当前登录用户，
// 思考：Controller 方法参数写 @RequestAttribute User loginUser 就能直接拿到，不用再查库
@Tag(name = "02-学生端", description = "岗位搜索、投递、简历（角色：student）")
@RestController
@RequestMapping("/api/student")
public class StudentController {

    // 思考：一个 Controller 可以依赖多个 Service：岗位搜索依赖 JobService、
    // 思考：投递依赖 ApplyService、简历依赖 ResumeService，各管一摊
    private final JobService jobService;
    private final ApplyService applyService;
    private final ResumeService resumeService;

    // 思考：构造器注入三个 Service
    public StudentController(JobService jobService, ApplyService applyService, ResumeService resumeService) {
        this.jobService = jobService;
        this.applyService = applyService;
        this.resumeService = resumeService;
    }

    @Operation(summary = "岗位搜索", description = "只返回发布中的岗位；三个条件可不传；支持分页")
    // 思考：岗位搜索：GET /api/student/jobs?keyword=Java&city=昆明&type=实习&page=1&size=10
    // 思考：@RequestParam(required = false)：URL 问号后的查询参数，不传就是 null = 不按这个条件筛
    // 思考：page/size 是分页参数，不传后端默认第 1 页、每页 10 条（见 JobService.search）
    @GetMapping("/jobs")
    public Result<Map<String, Object>> searchJobs(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String city,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        // 思考：搜索逻辑（含 Redis 缓存 + 分页）封装在 JobService.search，Controller 只透传参数
        return Result.ok(jobService.search(keyword, city, type, page, size));
    }

    @Operation(summary = "岗位详情")
    // 思考：岗位详情：GET /api/student/jobs/1
    // 思考：@PathVariable 取路径中的 {id} 变量：请求 /jobs/5 时 id=5
    @GetMapping("/jobs/{id}")
    public Result<Map<String, Object>> jobDetail(@PathVariable Long id) {
        // 思考：detail 返回岗位 + 企业名（前端岗位卡片显示"XX公司"）
        return Result.ok(jobService.detail(id));
    }

    @Operation(summary = "投递简历", description = "岗位须在招；同一岗位不可重复投递")
    // 思考：投递简历：POST /api/student/applies  body: {"jobId":1}
    // 思考：前端传 JSON，@RequestBody 转成 Map，取出 jobId
    @PostMapping("/applies")
    public Result<?> apply(@RequestAttribute User loginUser, @RequestBody Map<String, Long> req) {
        // 思考：loginUser.getId() 是服务端登录用户 id，不信任前端传的——防伪造他人身份投递
        applyService.apply(loginUser.getId(), req.get("jobId"));
        return Result.ok("投递成功");
    }

    @Operation(summary = "我的投递记录", description = "含状态、AI 评分与理由、所投简历内容；支持分页")
    // 思考：我的投递记录（含投递状态和 AI 评分）：GET /api/student/applies?page=1&size=10
    @GetMapping("/applies")
    public Result<Map<String, Object>> myApplies(@RequestAttribute User loginUser,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        // 思考：listByStudent 只查当前学生的投递，拼装岗位/简历/AI 评分，返回 {records,total}
        return Result.ok(applyService.listByStudent(loginUser.getId(), page, size));
    }

    @Operation(summary = "我的简历", description = "没有则返回 null")
    // 思考：我的简历：GET /api/student/resume（没有返回 null，前端据此显示"先填写简历"）
    @GetMapping("/resume")
    public Result<Resume> myResume(@RequestAttribute User loginUser) {
        return Result.ok(resumeService.getByUserId(loginUser.getId()));
    }

    @Operation(summary = "保存简历", description = "有则更新，无则新建")
    // 思考：保存简历：PUT /api/student/resume（有则更新，无则新建，逻辑在 ResumeService.save）
    // 思考：请求体直接就是简历对象本身：@RequestBody 自动把 JSON 转成 Resume 实体
    @PutMapping("/resume")
    public Result<?> saveResume(@RequestAttribute User loginUser, @RequestBody Resume resume) {
        // 思考：userId 以服务端登录用户为准，Service 内部强设，忽略前端传的
        resumeService.save(loginUser.getId(), resume);
        return Result.ok("简历保存成功");
    }
}