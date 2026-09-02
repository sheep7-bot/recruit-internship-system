package com.recruit.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 思考：登录+角色校验拦截器，整个项目的"安检门"
// 思考：所有 /api/** 请求都要先过这里（WebConfig 里注册），通过校验才放行到 Controller
// 思考：实现 HandlerInterceptor 接口并重写 preHandle，返回 true 放行 / false 拦下
@Component
public class AuthInterceptor implements HandlerInterceptor {

    // 思考：会话存储对象，用 token 反查"当前是谁在请求"
    private final TokenStore tokenStore;

    // 思考：Jackson 的 JSON 转换器，用于拦截失败时手动往响应里写一段 JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 思考：构造器注入，Spring 自动把 TokenStore 实例传进来（两个都是 @Component）
    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    // 思考：preHandle 在 Controller 方法执行【之前】运行，是最常用的拦截时机
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 思考：从请求头取 token（前端 axios 拦截器每次请求自动带上）
        // 思考：用 token 去 Redis 反查用户信息——查得到说明登录有效，查不到说明没登录/已过期
        User user = tokenStore.get(request.getHeader("token"));

        // 思考：没登录 → 写入 401 错误 JSON 并拦截，Controller 根本不会执行
        if (user == null) {
            writeJson(response, ResultCode.UNAUTHORIZED);
            return false;   // 思考：false = 拦截，请求到此为止
        }

        // 思考：按路径前缀判断当前请求需要的角色（路由即权限，一目了然）
        String uri = request.getRequestURI();
        String needRole;
        if (uri.startsWith("/api/student")) needRole = "student";
        else if (uri.startsWith("/api/company")) needRole = "company";
        else if (uri.startsWith("/api/admin")) needRole = "admin";
        else if (uri.startsWith("/api/ai")) needRole = "student";
        else needRole = null;   // 思考：不在以上前缀里的路径只要求登录，不限角色

        // 思考：角色不匹配 → 403 拒绝。前端收到后会自动把用户送回对应工作台
        if (needRole != null && !needRole.equals(user.getRole())) {
            writeJson(response, ResultCode.FORBIDDEN);
            return false;
        }

        // 思考：校验全部通过 → 把当前用户挂到 request 属性上
        // 思考：Controller 里用 @RequestAttribute("loginUser") 就能直接拿到，不用再查一次库
        request.setAttribute("loginUser", user);
        return true;   // 思考：true = 放行，继续执行 Controller
    }

    // 思考：拦截失败时手动往响应里写 JSON（此时不在 Controller 里，不能用 @RestController 的自动转换）
    // 思考：HTTP 层面仍返回 200，用业务码区分成败——前端 axios 拦截器统一按 code 判断，处理最简单
    private void writeJson(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=utf-8");
        // 思考：直接复用 Result 的统一结构，保证前端拿到的错误格式永远一致
        objectMapper.writeValue(response.getWriter(), Result.error(resultCode));
    }
}
