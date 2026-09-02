package com.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruit.entity.Apply;
import com.recruit.entity.AiScore;
import com.recruit.entity.Company;
import com.recruit.entity.Job;
import com.recruit.entity.Resume;
import com.recruit.entity.User;
import com.recruit.mapper.ApplyMapper;
import com.recruit.mapper.AiScoreMapper;
import com.recruit.mapper.CompanyMapper;
import com.recruit.mapper.JobMapper;
import com.recruit.mapper.ResumeMapper;
import com.recruit.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 思考：投递服务——投递、查看、状态流转，外加"跨公司数据隔离"的校验
// 思考：本类最有价值的设计：所有企业侧操作都要 checkJobOwned 校验岗位归属，
// 思考：否则 B 公司的 HR 改一下 jobId 就能看 A 公司的候选人——典型的水平越权漏洞
@Service
public class ApplyServiceImpl implements ApplyService {

    // 思考：五张表的数据访问对象——投递列表要"拼装"岗位、学生、简历、评分多个表的数据
    private final ApplyMapper applyMapper;
    private final JobMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final UserMapper userMapper;
    private final AiScoreMapper aiScoreMapper;
    private final CompanyMapper companyMapper;

    // 思考：构造器注入全部依赖
    public ApplyServiceImpl(ApplyMapper applyMapper, JobMapper jobMapper, ResumeMapper resumeMapper,
                        UserMapper userMapper, AiScoreMapper aiScoreMapper, CompanyMapper companyMapper) {
        this.applyMapper = applyMapper;
        this.jobMapper = jobMapper;
        this.resumeMapper = resumeMapper;
        this.userMapper = userMapper;
        this.aiScoreMapper = aiScoreMapper;
        this.companyMapper = companyMapper;
    }

    /**
     * 思考：学生投递。两道校验：岗位必须"发布中" + 同一岗位不能重复投
     */
    public void apply(Long studentId, Long jobId) {
        // 思考：校验一：岗位必须 status=1 发布中（待审核/已下架的投不了）
        Job job = jobMapper.selectById(jobId);
        if (job == null || job.getStatus() == null || job.getStatus() != 1) {
            throw new RuntimeException("该岗位不在招聘中");
        }
        // 思考：校验二：同一学生对同一岗位只能有一条投递记录（selectCount 查重）
        Long count = applyMapper.selectCount(new LambdaQueryWrapper<Apply>()
                .eq(Apply::getStudentId, studentId)
                .eq(Apply::getJobId, jobId));
        if (count > 0) {
            throw new RuntimeException("已投递过该岗位，不要重复投递");
        }
        // 思考：两道校验都过 → 创建投递记录，初始状态"已投递"
        Apply apply = new Apply();
        apply.setStudentId(studentId);
        apply.setJobId(jobId);
        apply.setStatus("已投递");
        applyMapper.insert(apply);
    }

    /**
     * 思考：学生查看自己的投递记录（我的投递页）
     * 思考：每条记录拼装：岗位信息 + 投递状态 + AI 评分 + 自己投的简历内容
     * 思考：返回 {records: 本页数据, total: 总条数}，前端渲染分页条
     */
    public Map<String, Object> listByStudent(Long studentId, Integer page, Integer size) {
        // 思考：分页参数兜底——默认第 1 页、每页 10 条
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        // 思考：分页查该学生的投递，最新的排前面（MP 自动拼 LIMIT）
        Page<Apply> applyPage = applyMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Apply>()
                        .eq(Apply::getStudentId, studentId)
                        .orderByDesc(Apply::getApplyTime));
        List<Apply> applies = applyPage.getRecords();
        // 思考：简历是学生自己的，查一次循环复用（不用每条投递都查一遍）
        Resume myResume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, studentId)
                .orderByDesc(Resume::getId)
                .last("limit 1"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Apply apply : applies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", apply.getId());
            item.put("status", apply.getStatus());
            item.put("applyTime", apply.getApplyTime());
            // 思考：带上简历内容——前端"查看投递简历"抽屉显示的就是它
            item.put("resumeContent", myResume == null ? "" : myResume.getContent());
            item.put("skills", myResume == null ? "" : myResume.getSkills());
            // 思考：补岗位信息（标题/城市/薪资），岗位可能已被删除，判空保护
            Job job = jobMapper.selectById(apply.getJobId());
            if (job != null) {
                item.put("jobTitle", job.getTitle());
                item.put("city", job.getCity());
                item.put("salary", job.getSalary());
            }
            // 思考：把 AI 评分带回去——学生能看到自己被 AI 打了多少分和理由
            AiScore score = aiScoreMapper.selectOne(new LambdaQueryWrapper<AiScore>()
                    .eq(AiScore::getApplyId, apply.getId()));
            if (score != null) {
                item.put("aiScore", score.getScore());
                item.put("aiReason", score.getReason());
            }
            result.add(item);
        }
        // 思考：组装分页结构返回
        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("records", result);
        pageResult.put("total", applyPage.getTotal());
        return pageResult;
    }

    /**
     * 思考：企业查看某岗位的投递列表（候选人筛选页）
     * 思考：companyUserId 用来校验"这个岗位属于当前登录的企业"——
     * 思考：否则 B 公司改一下 jobId 就能看到 A 公司的候选人，属于水平越权
     * 思考：返回 {records: 本页数据, total: 总条数}，前端渲染分页条
     */
    public Map<String, Object> listByJob(Long companyUserId, Long jobId, Integer page, Integer size) {
        // 思考：第一道闸——岗位归属校验，跨公司直接拒绝
        checkJobOwned(companyUserId, jobId);
        // 思考：分页参数兜底——默认第 1 页、每页 10 条
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        // 思考：分页查该岗位的投递，最新的排前面
        Page<Apply> applyPage = applyMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Apply>()
                        .eq(Apply::getJobId, jobId)
                        .orderByDesc(Apply::getApplyTime));
        List<Apply> applies = applyPage.getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Apply apply : applies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", apply.getId());
            item.put("jobId", apply.getJobId());
            item.put("status", apply.getStatus());
            item.put("applyTime", apply.getApplyTime());

            // 思考：补学生姓名（查 t_user）
            User student = userMapper.selectById(apply.getStudentId());
            item.put("studentName", student == null ? "" : student.getName());

            // 思考：补学生简历（学校/学历/技能/原文）——HR"看简历"抽屉显示用
            Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                    .eq(Resume::getUserId, apply.getStudentId())
                    .orderByDesc(Resume::getId)
                    .last("limit 1"));
            if (resume != null) {
                item.put("school", resume.getSchool());
                item.put("edu", resume.getEdu());
                item.put("skills", resume.getSkills());
                item.put("resumeContent", resume.getContent());
            }

            // 思考：补 AI 评分（HR 点过 AI 筛选才有）
            AiScore score = aiScoreMapper.selectOne(new LambdaQueryWrapper<AiScore>()
                    .eq(AiScore::getApplyId, apply.getId()));
            if (score != null) {
                item.put("aiScore", score.getScore());
                item.put("aiReason", score.getReason());
            }
            result.add(item);
        }
        // 思考：组装分页结构返回
        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("records", result);
        pageResult.put("total", applyPage.getTotal());
        return pageResult;
    }

    /**
     * 思考：更新投递状态（企业端"已查看"/"邀约"按钮）
     * 思考：白名单校验状态值——只允许三种合法状态，防止传奇怪的值进库
     */
    public void updateStatus(Long companyUserId, Long applyId, String status) {
        if (!"已投递".equals(status) && !"被查看".equals(status) && !"面试邀约".equals(status)) {
            throw new RuntimeException("非法的投递状态");
        }
        Apply apply = applyMapper.selectById(applyId);
        if (apply == null) throw new RuntimeException("投递记录不存在");
        // 思考：归属校验——这条投递对应的岗位必须是当前企业发的
        checkJobOwned(companyUserId, apply.getJobId());
        apply.setStatus(status);
        applyMapper.updateById(apply);
    }

    /**
     * 思考：岗位归属校验——多公司数据隔离的关键，所有企业侧操作共用
     * 思考：岗位不存在 或 不属于当前登录企业 → 一律拒绝
     */
    private void checkJobOwned(Long companyUserId, Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) throw new RuntimeException("岗位不存在");
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getUserId, companyUserId));
        if (company == null || !job.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("该岗位不属于当前企业，无权操作");
        }
    }
}
