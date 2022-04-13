package com.janwes.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.janwes.utils.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Janwes
 * @version 1.0.0
 * @package com.janwes.config
 * @date 2022/3/19 17:19
 * @description druid数据源连接池配置类
 */
@Configuration
public class DataSourceConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfiguration.class);

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DruidDataSource masterDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(username);
        // 密文解密
        dataSource.setPassword(SecurityUtil.decrypt(password));
        dataSource.setDriverClassName(driverClassName);
        LOGGER.info("===> initialize DruidDataSource,connect to MySQL server success......");
        return dataSource;
    }
}
