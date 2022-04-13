package com.janwes.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Janwes
 * @version 1.0
 * @package com.janwes.config
 * @date 2021/8/16 15:33
 * @description mybatis plus分页插件配置类
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 3.4.0之前的版本使用此分页拦截器
     *
     * @return
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }

    /**
     * 3.4.0之后提供的分页拦截器的配置方式
     *
     * @return
     */
   /*@Bean
   public MybatisPlusInterceptor mybatisPlusInterceptor(){
       MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
       mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
       return mybatisPlusInterceptor;
   }*/
}
