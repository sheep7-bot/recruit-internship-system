package com.recruit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 思考：@SpringBootApplication 是三个注解的合体，Spring Boot 最核心的注解：
// 思考：1. @SpringBootConfiguration —— 声明这是配置类
// 思考：2. @EnableAutoConfiguration —— 自动装配：根据 pom.xml 引入的依赖自动创建大量对象
// 思考：   （引入 webmvc 自动起 Tomcat、引入 data-redis 自动创建 Redis 连接对象）
// 思考：3. @ComponentScan —— 组件扫描：扫描本类所在包 com.recruit 及其子包下所有带
// 思考：   @Component/@Service/@RestController 等注解的类，注册进 Spring 容器统一管理
// 思考：   ——所以本项目所有代码都必须放在 com.recruit 包下，放外面就扫不到了
@SpringBootApplication
public class RecruitApplication {

    // 思考：Java 程序入口。SpringApplication.run 会依次完成：
    // 思考：1. 创建 Spring 容器（IoC 容器，整个项目所有对象都由它创建和管理）
    // 思考：2. 扫描并注册所有组件（Controller/Service/Mapper/配置类……）
    // 思考：3. 启动内嵌 Tomcat 监听 8081 端口（端口在 application.yml 里配置）
    // 思考：看到控制台输出 "Started RecruitApplication" 就代表后端启动成功
    public static void main(String[] args) {
        SpringApplication.run(RecruitApplication.class, args);
    }

}
