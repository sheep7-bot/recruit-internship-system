package com.recruit.common;

// 思考：用枚举封装业务状态码和默认提示语，好处有三：
// 思考：1. 状态码不再以"魔法数字"（0/1/401/403）散落在代码各处，改一处全局生效
// 思考：2. code 和默认 msg 绑定在一起，不会出现"码是 401 提示语却写错"的乌龙
// 思考：3. 新增业务状态时只需在枚举里加一行，所有调用方通过 Result.error(ResultCode.XXX) 使用
public enum ResultCode {

    // 思考：0 = 成功，沿用项目"0 即成功"的约定，前端 axios 拦截器按 code !== 0 统一判错
    SUCCESS(0, "success"),

    // 思考：1 = 通用业务失败，具体原因由抛异常时传入的 msg 决定（如"用户名或密码错误"）
    ERROR(1, "系统繁忙，请稍后再试"),

    // 思考：401 = 未登录/token 失效，拦截器校验 token 失败时使用，前端收到后踢回登录页
    UNAUTHORIZED(401, "未登录或登录已过期"),

    // 思考：403 = 已登录但角色不对（如学生号访问企业接口），前端收到后自动跳回对应工作台
    FORBIDDEN(403, "没有权限访问该功能");

    // 思考：final 保证状态码一旦创建不可被篡改，配合枚举本身的不可实例化，天然线程安全
    private final int code;
    private final String msg;

    // 思考：枚举构造方法只能是 private，在上方常量定义处（如 SUCCESS(0, "success")）被调用
    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    // 思考：对外只提供 getter，不提供 setter——状态码是常量，不允许中途修改
    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
