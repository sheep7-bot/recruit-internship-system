package com.recruit.config;

import com.recruit.common.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 思考：Web 配置类——注册登录拦截器 + 解决前后端跨域，两件 MVC 层面的全局配置
// 思考：@Configuration 表示这是配置类，Spring 启动时会读取里面的配置方法
// 思考：实现 WebMvcConfigurer 接口 = 声明"我要定制 Spring MVC 的默认行为"（重写哪个方法就定制哪块）
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 思考：要注册的登录拦截器（com.recruit.common.AuthInterceptor）
    private final AuthInterceptor authInterceptor;

    // 思考：构造器注入，Spring 自动把拦截器实例传进来
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 思考：拦截器注册——决定"哪些请求要先过 AuthInterceptor 这一关"
     * 思考：addPathPatterns("/api/**") 拦截所有业务接口；excludePathPatterns 放行登录注册
     * 思考：为什么放行 /api/auth/**？——登录注册时用户还没有 token，
     * 思考：如果也拦截就死循环了（没登录 → 要 token → 拿 token 又得先登录）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }

    /**
     * 思考：跨域（CORS）配置
     * 思考：浏览器安全规则：页面只能请求"同源"（协议+域名+端口全相同）的接口
     * 思考：本项目前端 5173、后端 8081，端口不同 = 不同源，浏览器会拦截前端发出的请求
     * 思考：所以必须在后端声明"我允许前端的跨域请求"，否则前端报 CORS 错误
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 思考：允许任意来源（实训图省事；生产环境应只写前端的具体域名）
                .allowedOriginPatterns("*")
                // 思考：允许的 HTTP 方法，项目里增删改查全部用到；OPTIONS 是浏览器跨域预检请求
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 思考：允许携带任意请求头——包括我们自定义的 token 请求头，不加这个 token 传不过去
                .allowedHeaders("*");
    }
}
