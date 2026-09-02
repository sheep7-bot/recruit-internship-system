package com.recruit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;

// 思考：AI 配置类——创建全局唯一的 ChatClient（调用大模型的"客户端"对象）
// 思考：@Configuration + @Bean 的组合：Bean 方法返回的对象交给 Spring 容器管理，
// 思考：全项目任何类在构造器里声明 ChatClient 参数都能拿到同一个实例（单例共享）
@Configuration
public class ChatClientConfig {

    /**
     * 思考：注册全局 ChatClient Bean
     * 思考：builder 是 Spring AI 自动装配的构建器——它已经读取了 application.yml 里
     * 思考：spring.ai.openai 下的 api-key（千问的 key）、base-url（DashScope 兼容地址）、model（qwen-plus），
     * 思考：所以我们只管 build()，底层 HTTP 请求、鉴权、JSON 解析全都不用自己写
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // 思考：defaultSystem 设置全局"系统提示词"（System Prompt）
        // 思考：系统提示词对用户不可见，但每次对话都会生效，相当于给大模型固定人设和规矩：
        // 思考：1. 限定身份 = 招聘系统的 AI 助手；2. 限定话题 = 求职招聘；3. 限定风格 = 中文简洁专业
        // 思考：好处：提示词只写这一处，问答/解析/评分/推荐所有 AI 功能自动遵守，不用每个地方重复写
        return builder.defaultSystem("""
                你是"实习招聘及智能分析系统"的 AI 助手，负责简历解析、人岗匹配评分和求职答疑。
                回答要求：简洁专业，用中文，紧扣求职/招聘/实习主题；
                如果用户问的问题与求职无关，礼貌地把话题引导回求职上来。
                """).build();
    }
}
