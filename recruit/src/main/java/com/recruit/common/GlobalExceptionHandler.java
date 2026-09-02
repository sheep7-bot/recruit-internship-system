package com.recruit.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 思考：全局异常处理器。没有它的话，Service 抛出的异常会变成 HTTP 500 + 一堆英文堆栈
// 思考：返回给前端，用户看不懂还会泄露服务器内部信息（表名、SQL、路径）
// 思考：有了它之后，业务代码里可以放心直接 throw，异常统一在这里收口转成友好 JSON
//
// 思考：@RestControllerAdvice = @ControllerAdvice（对所有 Controller 生效的切面）
// 思考：                 + @ResponseBody（方法返回值自动转 JSON 写进响应体）
// 思考：@Slf4j 是 Lombok 注解：自动生成日志对象 log（log.info/warn/error），输出到控制台和日志文件
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 思考：处理业务异常——项目把"预期内的错误"（密码错误、重复投递……）都定义为 RuntimeException，
    // 思考：Service 里直接 throw，这里捕获后把原因原样告诉前端（这些信息本来就是给用户看的）
    // 思考：warn 级别：业务异常是正常现象，用 warn 记录方便和真正的系统错误区分开
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleBusiness(RuntimeException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    // 思考：兜底方法——Exception 是所有异常的父类，上面没接住的都会进来
    // 思考：这类是真正的程序 bug（空指针、数据库连不上……），必须 error 级别 + 打印完整堆栈 e 便于排查
    // 思考：但返回给前端的只有一句模糊提示——绝不能把内部细节（表名、SQL）暴露给外界
    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.ERROR);
    }
}
