package com.recruit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 思考：接口文档（Knife4j/OpenAPI）配置类——只负责文档的"元信息"（标题/描述/作者/版本）
// 思考：文档的正文内容不是在这里写的，而是框架自动扫描 Controller 上的
// 思考：@Tag（类分组）和 @Operation（接口说明）注解生成的，改接口时记得同步改注解
// 思考：查看地址：后端启动后打开 http://localhost:8081/doc.html
@Configuration
public class OpenApiConfig {

    /**
     * 思考：注册 OpenAPI 元信息 Bean，springdoc 读取后展示在文档页面顶部
     * 思考：description 用三引号文本块写多行使用说明，让看文档的人不用翻 README 就知道怎么调试
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                // 思考：文档页面的大标题
                .title("实习招聘及智能分析系统 接口文档")
                // 思考：简介区：写清 token 的用法和统一返回结构，看文档的人最需要的两条信息
                .description("""
                        学生/企业/管理员三端 + AI 智能筛选的校园实习招聘平台。
                        通用说明：
                        1. 除 /api/auth/** 外，所有接口都需要在请求头携带 token（登录接口返回）
                        2. 统一返回结构：{code: 0成功/非0失败, msg: 提示, data: 数据}
                        3. AI 类接口依赖通义千问，失败时自动降级为友好提示
                        """)
                .version("1.0.0")
                // 思考：作者/联系人信息
                .contact(new Contact().name("李翔（组长）")));
    }
}
