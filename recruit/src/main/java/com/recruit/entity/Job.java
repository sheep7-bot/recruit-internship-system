package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 思考：岗位实体类——对应数据库表 t_job
// 思考：status 状态流转体现"平台审核"机制：
// 思考：  企业发布 status=0（待审核）→ 管理员通过=1（发布中，学生可见）/ 驳回=2
// 思考：  → 企业可主动下架=2（学生端不可见不可投）
// 思考：skills/edu_req 两个"要求"字段是 AI 双通道筛选的规则依据（见 AiMatchService.checkRule）
@Data
@TableName("t_job")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;           // 思考：主键

    private Long companyId;    // 思考：发布企业（t_company.id）——企业只能操作自己公司的岗位
    private String title;      // 思考：岗位名称
    private String city;       // 思考：工作城市（学生端筛选条件）
    private String salary;     // 思考：薪资范围
    private String type;       // 思考：类型 实习/校招（学生端筛选条件）
    private String skills;     // 思考：技能要求逗号分隔——规则筛选的技能比对依据
    private String eduReq;     // 思考：学历要求——规则筛选的学历比对依据
    private String description;// 思考：岗位描述（大模型评分时的参考资料）
    private Integer status;    // 思考：0 待审核 / 1 发布中 / 2 已下架或驳回
    private LocalDateTime createTime;  // 思考：创建时间
}
