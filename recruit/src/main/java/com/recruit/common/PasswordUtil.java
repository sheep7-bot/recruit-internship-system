package com.recruit.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 思考：密码工具——统一封装 BCrypt 加密/校验，业务代码里不直接碰 BCryptPasswordEncoder
// 思考：为什么用 BCrypt 而不是 MD5/SHA？
// 思考：  1. BCrypt 自带随机盐（同一密码每次加密结果都不同），MD5 相同密码永远相同，撞库/彩虹表一打一个准
// 思考：  2. BCrypt 计算慢（故意设计的），暴力破解成本高；MD5 算得太快，一秒能试几亿次
// 思考：  3. 强度参数可调（cost=10），以后硬件变快可以调大，老哈希仍可校验
// 思考：兼容历史明文数据：matches 时若库里是明文直接比较，命中后由 UserService 自动升级为 BCrypt
public class PasswordUtil {

    // 思考：BCrypt 编码器单例。cost=10 是 Spring 默认强度（2^10 次计算），实训项目够用
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    // 思考：注册时调用——明文密码加密后再存库
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 思考：登录时校验密码
     * 思考：库里是 BCrypt（$2 开头）→ 用 BCrypt 算法匹配
     * 思考：库里还是明文（老数据）→ 直接比较（兼容，命中后由 Service 升级）
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        return rawPassword != null && rawPassword.equals(storedPassword);
    }
}