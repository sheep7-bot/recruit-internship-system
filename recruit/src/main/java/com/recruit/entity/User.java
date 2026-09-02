package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 思考：用户实体类——对应数据库表 t_user，表里一行数据 = 内存里一个 User 对象
// 思考：三类用户（student 学生 / company 企业 / admin 管理员）共用一张表，用 role 字段区分
// 思考：为什么不拆三张表？——三者登录逻辑完全一样（用户名+密码），拆表登录要查三次库；
// 思考：企业特有的信息放 t_company 扩展表，通过 user_id 关联（见 Company 实体）
// 思考：字段映射规则：MyBatis-Plus 默认开启驼峰转下划线，createTime 自动对应 create_time
// 思考：所以不需要写 @TableField 手动映射（除非字段名对不上）
@Data
// 思考：@TableName 指定实体对应的表名——类名叫 User 但表叫 t_user，对不上必须显式指定
// 思考：反过来如果类名和表名一致（如 Job 类对应 job 表）就可以省略
@TableName("t_user")
public class User {

    // 思考：@TableId 标记主键字段；IdType.AUTO 表示主键由数据库自增
    // 思考：INSERT 时不传 id，插入后数据库分配并自动回填到对象里（插入完就能拿到新用户 id）
    @TableId(type = IdType.AUTO)
    private Long id;              // 思考：主键

    private String username;      // 思考：登录名（唯一，注册时查重校验）
    private String password;      // 思考：密码（实训明文存储便于调试；生产环境必须加密如 BCrypt）
    private String role;          // 思考：角色 student/company/admin——拦截器按它判断接口访问权限
    private String name;          // 思考：姓名（学生真名 / 企业端显示的 HR 姓名）
    private String phone;         // 思考：电话
    private Integer status;       // 思考：账号状态 1 正常 0 禁用（管理员可在后台禁用，登录时校验）

    // 思考：LocalDateTime 是 Java 8+ 推荐的时间类（比老 Date 好用且线程安全）
    private LocalDateTime createTime;  // 思考：注册时间，数据库 DEFAULT CURRENT_TIMESTAMP 自动填
}
