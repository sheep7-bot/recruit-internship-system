package com.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruit.entity.Resume;
import com.recruit.mapper.ResumeMapper;
import org.springframework.stereotype.Service;

// 思考：简历服务——最短的一个 Service，适合作为阅读业务层的入门
// 思考：核心规则：每个学生只有一份简历，"有就更新、没有就新建"（save 方法）
@Service
public class ResumeServiceImpl implements ResumeService {

    // 思考：简历表的数据访问对象
    private final ResumeMapper resumeMapper;

    // 思考：构造器注入
    public ResumeServiceImpl(ResumeMapper resumeMapper) {
        this.resumeMapper = resumeMapper;
    }

    /**
     * 思考：按学生查简历，没有返回 null（前端据此显示"先填写简历"）
     * 思考：orderByDesc + last("limit 1") 是保险写法：万一历史数据出现重复，永远取最新一份
     */
    public Resume getByUserId(Long userId) {
        return resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getId)
                .last("limit 1"));   // 思考：last 直接在 SQL 末尾追加一段原生 SQL，这里用来限一条
    }

    /**
     * 思考：保存/更新简历（学生端"保存简历"按钮调它）
     * 思考：按"该学生是否已有简历"决定插入还是更新，对前端屏蔽了这两者的区别
     */
    public void save(Long userId, Resume form) {
        // 思考：先查现有简历
        Resume exist = getByUserId(userId);
        if (exist == null) {
            // 思考：没有 → 新建。setId(null) 防止前端传来假 id 干扰自增主键
            form.setId(null);
            form.setUserId(userId);   // 思考：userId 以服务端登录用户为准，绝不信任前端传的
            resumeMapper.insert(form);
        } else {
            // 思考：有 → 按已有 id 更新
            form.setId(exist.getId());
            form.setUserId(userId);
            resumeMapper.updateById(form);
            // 思考：updateById 只更新非 null 字段——前端没填的字段不会把数据库里的旧值冲掉
        }
    }

    /**
     * 思考：按 id 查（AI 简历解析用，解析完回填后走 update 保存）
     */
    public Resume getById(Long id) {
        return resumeMapper.selectById(id);
    }

    /**
     * 思考：AI 解析完成后回填结构化字段（edu/skills/experience/name/age...）
     */
    public void update(Resume resume) {
        resumeMapper.updateById(resume);
    }
}
