package com.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recruit.entity.Company;
import com.recruit.entity.Job;
import com.recruit.mapper.CompanyMapper;
import com.recruit.mapper.JobMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 思考：岗位服务——发布、搜索、审核、下架，外加 Redis 搜索缓存
// 思考：本类有两个值得重点学的点：
// 思考：  1. getOwned 归属校验——企业只能操作自己公司的岗位（防越权）
// 思考：  2. search 的 Redis 缓存三步曲——查缓存 → 未命中查库 → 写缓存，岗位变更时主动清缓存
@Service
public class JobServiceImpl implements JobService {

    // 思考：岗位表数据访问对象
    private final JobMapper jobMapper;
    // 思考：企业表数据访问对象——查企业档案、给岗位列表补企业名都用它
    private final CompanyMapper companyMapper;
    // 思考：Redis 操作模板——岗位搜索缓存的读写和删除都靠它
    private final StringRedisTemplate redis;
    // 思考：缓存里的 JSON 和 Java 对象互转用；LocalDateTime 字段需要 JavaTimeModule 支持
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // 思考：缓存 key 统一前缀——清缓存时按前缀批量删除
    private static final String CACHE_PREFIX = "cache:jobs:";
    // 思考：缓存有效期 5 分钟——即使主动失效逻辑有遗漏，脏数据最多活 5 分钟（兜底保险）
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // 思考：构造器注入三个依赖
    public JobServiceImpl(JobMapper jobMapper, CompanyMapper companyMapper, StringRedisTemplate redis) {
        this.jobMapper = jobMapper;
        this.companyMapper = companyMapper;
        this.redis = redis;
    }

    /**
     * 思考：企业发布岗位：新岗位 status=0 待审核，管理员审核通过后才上架（status=1）
     * 思考：companyId 以服务端登录用户为准——企业发的岗位自动挂到它自己公司名下
     */
    public void publish(Long companyUserId, Job job) {
        // 思考：根据登录用户查出企业档案（拿 companyId）
        Company company = getByUserId(companyUserId);
        job.setId(null);          // 思考：清掉前端可能传来的假 id，主键交给数据库自增
        job.setCompanyId(company.getId());
        job.setStatus(0);         // 思考：强制待审核状态，企业不能自己跳过审核
        jobMapper.insert(job);
        evictJobCache();          // 思考：岗位数据变了，学生端搜索缓存作废
    }

    /**
     * 思考：企业修改自己的岗位
     * 思考：getOwned 会校验"这个岗位属于这家企业"，防止越权改别人的
     */
    public void update(Long companyUserId, Job form) {
        Job job = getOwned(companyUserId, form.getId());
        // 思考：企业不能自己改审核状态——上下架/通过只能由管理员或下架动作触发
        form.setStatus(job.getStatus());
        form.setCompanyId(job.getCompanyId());
        jobMapper.updateById(form);
        evictJobCache();
    }

    /**
     * 思考：企业下架岗位：status 置 2，学生端立即不可见
     */
    public void offline(Long companyUserId, Long jobId) {
        Job job = getOwned(companyUserId, jobId);
        job.setStatus(2);
        jobMapper.updateById(job);
        evictJobCache();
    }

    /**
     * 思考：岗位搜索（学生端）：只搜"发布中"，支持关键词/城市/类型组合筛选 + 分页
     * 思考：Redis 缓存三步曲——先查缓存命中就直接返回；未命中查库并写缓存 5 分钟
     * 思考：为什么缓存？学生端搜索是全系统最高频的读操作，没必要每次都打数据库
     * 思考：返回结构改成 {records: 本页数据, total: 总条数}——前端拿到 total 渲染分页条
     */
    public Map<String, Object> search(String keyword, String city, String type, Integer page, Integer size) {
        // 思考：分页参数兜底——前端不传就用第 1 页、每页 10 条；传负数也纠正成合法值
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        // 思考：第一步——把筛选条件拼进缓存 key：相同条件+相同页码的搜索共享同一份缓存
        // 思考：null 转空字符串，保证"没传条件"和"传了空串"命中同一个 key
        // 思考：页码拼进 key 的原因：不同页的数据不同，不能混着缓存
        String cacheKey = CACHE_PREFIX + (keyword == null ? "" : keyword)
                + "|" + (city == null ? "" : city) + "|" + (type == null ? "" : type)
                + "|p" + page + "s" + size;
        // 思考：第二步——尝试读缓存。读失败（Redis 抖动）不影响主流程，直接查库
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                // 思考：命中！把缓存的 JSON 反序列化回 Map 直接返回（TypeReference 保留泛型信息）
                return mapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
        }
        // 思考：第三步——缓存未命中，查数据库
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, 1)   // 思考：只查"发布中"，待审核/已下架的学生看不见
                // 思考：eq 的第一个参数是布尔开关——条件为 false 时这个 WHERE 不拼接（动态条件）
                .eq(city != null && !city.isBlank(), Job::getCity, city)
                .eq(type != null && !type.isBlank(), Job::getType, type);
        if (keyword != null && !keyword.isBlank()) {
            // 思考：关键词同时模糊匹配岗位名称和技能要求（OR 关系），包在 and() 里避免和上面的条件打架
            wrapper.and(w -> w.like(Job::getTitle, keyword).or().like(Job::getSkills, keyword));
        }
        wrapper.orderByDesc(Job::getCreateTime);   // 思考：最新发布的排前面

        // 思考：selectPage 分页查询——MP 自动拼 LIMIT，total 是满足条件的总条数（分页条要用）
        // 思考：分页插件见 MybatisPlusConfig——不注册它，这里会查出全量
        Page<Job> jobPage = jobMapper.selectPage(new Page<>(page, size), wrapper);
        // 思考：组装返回：records = 本页岗位（补企业名），total = 总条数
        Map<String, Object> result = new HashMap<>();
        result.put("records", fillCompanyName(jobPage.getRecords()));
        result.put("total", jobPage.getTotal());

        // 思考：第四步——把结果序列化成 JSON 写入 Redis，5 分钟后自动过期
        try {
            redis.opsForValue().set(cacheKey, mapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * 思考：岗位详情
     */
    public Map<String, Object> detail(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) throw new RuntimeException("岗位不存在");
        // 思考：复用 fillCompanyName 给详情也补上企业名
        List<Map<String, Object>> list = fillCompanyName(List.of(job));
        return list.get(0);
    }

    /**
     * 思考：企业查看自己发布的全部岗位（含待审核/发布中/已下架各状态）
     */
    public List<Map<String, Object>> listByCompany(Long companyUserId) {
        Company company = getByUserId(companyUserId);
        List<Job> jobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, company.getId())
                .orderByDesc(Job::getCreateTime));
        return fillCompanyName(jobs);
    }

    /**
     * 思考：管理端按状态查岗位（null = 全部）
     * 思考：eq 的布尔开关在 status 为 null 时自动跳过——一个 wrapper 同时支持"查全部"和"按状态查"
     */
    public List<Map<String, Object>> listByStatus(Integer status) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .eq(status != null, Job::getStatus, status)
                .orderByDesc(Job::getCreateTime);
        return fillCompanyName(jobMapper.selectList(wrapper));
    }

    /**
     * 思考：管理员审核岗位：pass=true 通过上架（学生端立即可见），false 驳回
     */
    public void audit(Long jobId, boolean pass) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) throw new RuntimeException("岗位不存在");
        job.setStatus(pass ? 1 : 2);
        jobMapper.updateById(job);
        evictJobCache();   // 思考：上架/驳回影响学生端搜索结果，清缓存
    }

    /**
     * 思考：清除所有岗位搜索缓存——删除 cache:jobs: 开头的全部 key
     * 思考：keys() 是全库扫描，数据量小够用；生产大库应改用 SCAN 渐进扫描（面试可聊）
     */
    private void evictJobCache() {
        var keys = redis.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    /**
     * 思考：查当前登录企业的档案——企业端头部显示公司名称的 /company/info 接口用
     */
    public Company getCompanyByUserId(Long companyUserId) {
        return getByUserId(companyUserId);
    }

    /**
     * 思考：校验"这个岗位属于这家企业"——发布/编辑/下架共用，防越权的第一道闸
     * 思考：岗位不存在 或 不属于当前企业 都拒绝
     */
    private Job getOwned(Long companyUserId, Long jobId) {
        Company company = getByUserId(companyUserId);
        Job job = jobMapper.selectById(jobId);
        if (job == null || !job.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("岗位不存在或不属于当前企业");
        }
        return job;
    }

    // 思考：按登录用户查企业档案（多个方法共用）
    private Company getByUserId(Long companyUserId) {
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getUserId, companyUserId));
        if (company == null) throw new RuntimeException("企业信息不存在");
        return company;
    }

    /**
     * 思考：给岗位列表补上企业名称（前端岗位卡片要显示"XX公司"）
     * 思考：小数据量直接循环查库 + nameCache 缓存同一企业的重复查询；
     * 思考：数据量大了应该改关联查询或 Redis 缓存——这是面试可聊的优化点
     */
    private List<Map<String, Object>> fillCompanyName(List<Job> jobs) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 思考：本方法内的企业名小缓存：同公司多个岗位只查一次库
        Map<Long, String> nameCache = new HashMap<>();
        for (Job job : jobs) {
            // 思考：用 Map 组装返回结构（而不是新建 DTO 类）——字段名即 JSON 的 key
            Map<String, Object> item = new HashMap<>();
            item.put("id", job.getId());
            item.put("title", job.getTitle());
            item.put("city", job.getCity());
            item.put("salary", job.getSalary());
            item.put("type", job.getType());
            item.put("skills", job.getSkills());
            item.put("eduReq", job.getEduReq());
            item.put("description", job.getDescription());
            item.put("status", job.getStatus());
            item.put("createTime", job.getCreateTime());
            item.put("companyId", job.getCompanyId());
            // 思考：computeIfAbsent——key 不存在时才执行查库函数，存在就直接用缓存值
            item.put("companyName", nameCache.computeIfAbsent(job.getCompanyId(), cid -> {
                Company c = companyMapper.selectById(cid);
                return c == null ? "" : c.getName();
            }));
            result.add(item);
        }
        return result;
    }
}
