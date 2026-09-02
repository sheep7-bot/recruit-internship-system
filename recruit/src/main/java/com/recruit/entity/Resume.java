package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 思考：学生简历实体类——对应数据库表 t_resume，每个学生一份（有则更新无则新建）
// 思考：字段分两类，这个设计是 AI 功能的地基：
// 思考：  1. 原文类 content——学生粘贴/上传提取的简历全文（非结构化）
// 思考：  2. 结构化类 edu/skills/experience——AI 解析抽取的结果（结构化）
// 思考：为什么要分？HR 的规则筛选（学历/技能比对）必须用结构化字段，
// 思考：每次都拿全文去比对又慢又不准。AI 解析负责"非结构化 → 结构化"的转换
@Data
@TableName("t_resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;              // 思考：主键

    private Long userId;          // 思考：所属学生（t_user.id）
    private String name;          // 思考：姓名（AI 解析自动回填，也可手填）
    private String school;        // 思考：学校（AI 解析自动回填）
    private String edu;           // 思考：学历 大专/本科/硕士/博士——规则筛选的学历比对依据
    private Integer age;          // 思考：年龄
    private String skills;        // 思考：技能逗号分隔（如 Java,Spring Boot）——规则筛选的技能匹配依据
    private String experience;    // 思考：主要经历（AI 解析自动回填）
    private String content;       // 思考：简历全文——AI 解析的数据来源
    private LocalDateTime createTime;  // 思考：创建时间
}
