package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 思考：投递记录实体类——对应数据库表 t_apply
// 思考：学生和岗位是"多对多"关系：一个学生投多个岗位、一个岗位收多个投递
// 思考：关系型数据库表达多对多的标准做法：建一张中间表——t_apply 就是这张表
// 思考：每一行 = "某个学生 投递了 某个岗位"这一件事
// 思考：status 由企业 HR 推进：已投递 → 被查看 → 面试邀约（学生端渲染成三步进度条）
@Data
@TableName("t_apply")
public class Apply {

    @TableId(type = IdType.AUTO)
    private Long id;        // 思考：主键

    private Long studentId; // 思考：投递的学生（t_user.id）
    private Long jobId;     // 思考：被投递的岗位（t_job.id）
    private String status;  // 思考：投递状态 已投递/被查看/面试邀约
    private LocalDateTime applyTime;  // 思考：投递时间，数据库默认当前时间
}
