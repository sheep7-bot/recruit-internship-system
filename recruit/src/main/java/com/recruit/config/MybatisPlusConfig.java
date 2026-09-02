package com.recruit.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 思考：MyBatis-Plus 插件配置——本类唯一作用：注册"分页拦截器"
// 思考：为什么必须注册？MP 的 selectPage(Page, wrapper) 本身只是"声明"要分页，
// 思考：真正在 SQL 末尾追加 LIMIT 的是这个拦截器。不注册的话 selectPage 会查出全量数据，分页形同虚设
// 思考：@Configuration 标注这是配置类，Spring 启动时自动扫描里面的 @Bean
@Configuration
public class MybatisPlusConfig {

    // 思考：@Bean 把拦截器交给 Spring 容器管理，MyBatis-Plus 启动时会自动拾取它
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 思考：MybatisPlusInterceptor 是插件的"挂载点"，把各个 InnerInterceptor 挂进去
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 思考：分页插件本体，指定数据库类型 MySQL——分页 SQL 的方言（LIMIT ? OFFSET ?）由它生成
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}