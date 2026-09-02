package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 思考：企业实体类——对应数据库表 t_company，登录账号和公司档案分离
// 思考：每个企业账号（t_user 里 role=company）都有一份这里的企业档案，通过 user_id 一对一关联
// 思考：approved 入驻审核状态是本表核心字段，状态流转：
// 思考：  注册插入 approved=0（待审核）→ 管理员通过=1（可登录可发岗位）/ 驳回=2（登录被拒）
@Data
@TableName("t_company")
public class Company {

    @TableId(type = IdType.AUTO)
    private Long id;               // 思考：主键

    private Long userId;           // 思考：关联的登录账号（t_user.id），"这个档案是谁的"
    private String name;           // 思考：企业名称（企业端头部显示的就是它）
    private String industry;       // 思考：所属行业（IT服务/软件研发……）
    private String scale;          // 思考：公司规模（100-500人……）
    private String contact;        // 思考：联系人（一般就是 HR 姓名）
    private Integer approved;      // 思考：入驻审核 0 待审核 / 1 通过 / 2 驳回
    private LocalDateTime createTime;  // 思考：申请入驻时间
}
