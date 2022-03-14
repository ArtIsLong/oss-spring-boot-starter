package io.github.artislong.core.jdbc;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.zaxxer.hikari.HikariDataSource;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jdbc.model.JdbcOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * @author 陈敏
 * @version JdbcOssConfiguration.java, v 1.0 2022/3/11 21:35 chenmin Exp $
 * Created on 2022/3/11
 */
@Configuration
@ConditionalOnClass(JdbcTemplate.class)
@EnableConfigurationProperties({JdbcOssProperties.class})
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.JDBC + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class JdbcOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "jdbcOssClient";

    @Autowired
    private JdbcOssProperties jdbcOssProperties;

    @Bean
    public void jdbcOssClient() {
        Map<String, JdbcOssConfig> ossConfigMap = jdbcOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, jdbcOssClient(jdbcOssProperties));
        } else {
            ossConfigMap.forEach((name, ossConfig) -> SpringUtil.registerBean(name, jdbcOssClient(ossConfig)));
        }
    }

    public StandardOssClient jdbcOssClient(JdbcOssConfig jdbcOssConfig) {
        return new JdbcOssClient(jdbcTemplate(jdbcOssConfig), jdbcOssConfig);
    }

    public JdbcTemplate jdbcTemplate(JdbcOssConfig jdbcOssConfig) {
        Class<? extends DataSource> type = jdbcOssConfig.getType();
        if (ObjectUtil.isEmpty(type)) {
            type = HikariDataSource.class;
        }
        DataSource dataSource = DataSourceBuilder.create()
                .type(type)
                .driverClassName(jdbcOssConfig.getDriver())
                .url(jdbcOssConfig.getUrl())
                .username(jdbcOssConfig.getUsername())
                .password(jdbcOssConfig.getPassword())
                .build();

        return new JdbcTemplate(dataSource);
    }
}
