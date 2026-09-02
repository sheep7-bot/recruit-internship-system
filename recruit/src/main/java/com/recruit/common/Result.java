package com.recruit.common;

import lombok.Data;

// 思考：统一返回格式类。没有它的话，每个接口返回的 JSON 结构五花八门，前端要逐个适配；
// 思考：统一成 {code, msg, data} 之后，前端 axios 拦截器只需要写一次判断逻辑（code !== 0 就弹错）
@Data
public class Result<T> {

    // 思考：业务状态码（注意区分：这是业务层的 0/非0，和 HTTP 协议的 200/404 是两套体系）
    private int code;

    // 思考：提示信息，成功时是 success，失败时是给用户看的错误原因（前端直接弹窗展示）
    private String msg;

    // 思考：泛型 T 让 data 字段可以装任何类型——查用户装 User、查列表装 List，一个类通吃所有接口
    private T data;

    // 思考：成功快捷方法。static <T> 表示泛型由调用处传入的参数自动推断，调用方写 Result.ok(数据) 即可
    // 思考：双括号初始化 {{ }} = 外层创建匿名子类对象 + 内层实例初始化块，一行完成三个字段赋值
    public static <T> Result<T> ok(T data) {
        return new Result<T>() {{ setCode(0); setMsg("success"); setData(data); }};
    }

    // 思考：失败快捷方法一：业务异常场景，错误原因动态传入（配合 GlobalExceptionHandler 使用）
    public static <T> Result<T> error(String msg) {
        return new Result<T>() {{ setCode(ResultCode.ERROR.getCode()); setMsg(msg); setData(null); }};
    }

    // 思考：失败快捷方法二：错误码+默认提示语都从 ResultCode 枚举取，避免裸数字散落各处
    // 思考：拦截器里的 401/403 响应就是用这个方法构造的
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<T>() {{ setCode(resultCode.getCode()); setMsg(resultCode.getMsg()); setData(null); }};
    }
}
