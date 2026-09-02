package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 思考：AI 筛选评分实体类——对应数据库表 t_ai_score
// 思考：和投递是一对一关系：每条投递至多一条评分记录
// 思考：score 两种含义：大模型评分 0-100 / 规则筛选拒绝时固定存 0
// 思考：reason 对应两种内容：大模型生成的评分理由 / 规则拒绝原因（如"未通过规则筛选：学历要求 本科"）
// 思考：为什么独立建表不把分数放 t_apply 上？
// 思考：  1. 职责分离：投递是学生行为数据，评分是 AI 分析数据
// 思考：  2. 可重复筛选：重新筛选删旧分存新分，旧结论自动被覆盖
// 思考：  3. 并非所有投递都有评分：只有 HR 点过 AI 筛选的岗位才有记录
@Data
@TableName("t_ai_score")
public class AiScore {

    @TableId(type = IdType.AUTO)
    private Long id;      // 思考：主键

    private Long applyId; // 思考：对应的投递记录（t_apply.id）
    private Integer score;// 思考：匹配分 0-100
    private String reason;// 思考：评分理由或规则拒绝原因
    private LocalDateTime createTime;  // 思考：评分时间
}
