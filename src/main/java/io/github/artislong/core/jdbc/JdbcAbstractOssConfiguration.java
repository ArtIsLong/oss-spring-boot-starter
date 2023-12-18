package io.github.artislong.core.jdbc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.zaxxer.hikari.HikariDataSource;
import io.github.artislong.AbstractOssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jdbc.adapter.JdbcOssOperation;
import io.github.artislong.core.jdbc.adapter.JdbcOssOperationFactoryBean;
import io.github.artislong.core.jdbc.model.JdbcOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.function.ThreeConsumer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 陈敏
 * @version JdbcOssConfiguration.java, v 1.0 2022/3/11 21:35 chenmin Exp $
 * Created on 2022/3/11
 */
@SpringBootConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@EnableConfigurationProperties({JdbcOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.JDBC + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class JdbcAbstractOssConfiguration extends AbstractOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "jdbcOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        JdbcOssProperties jdbcOssProperties = getOssProperties(JdbcOssProperties.class, OssConstant.OssType.JDBC);
        Map<String, JdbcOssConfig> ossConfigMap = jdbcOssProperties.getOssConfig();
        // 开启Jdbc存储的同时，未配置对应的Jdbc存储数据库连接，将采用默认数据源
        if (ossConfigMap.isEmpty() && isEmptyForOssConfig(jdbcOssProperties)) {
            DataSource dataSource = SpringUtil.getBean(DataSource.class);
            if (ObjectUtil.isEmpty(dataSource)) {
                throw new OssException("未配置数据源，请检查！");
            }
            consumer.accept(DEFAULT_BEAN_NAME, JdbcOssClient.class, buildBeanProMap(jdbcOssProperties, jdbcOssOperation(dataSource)));
        }
        if (ossConfigMap.isEmpty() && !isEmptyForOssConfig(jdbcOssProperties)) {
            registerJdbcOssClient(DEFAULT_BEAN_NAME, jdbcOssProperties, consumer);
        } else {
            Set<Map.Entry<String, JdbcOssConfig>> entrySet = ossConfigMap.entrySet();
            for (Map.Entry<String, JdbcOssConfig> jdbcOssConfigEntry : entrySet) {
                registerJdbcOssClient(jdbcOssConfigEntry.getKey(), jdbcOssConfigEntry.getValue(), consumer);
            }
        }
    }

    private boolean isEmptyForOssConfig(JdbcOssConfig jdbcOssConfig) {
        return ObjectUtil.isEmpty(jdbcOssConfig.getDriver()) && ObjectUtil.isEmpty(jdbcOssConfig.getType()) &&
                ObjectUtil.isEmpty(jdbcOssConfig.getUrl()) && ObjectUtil.isEmpty(jdbcOssConfig.getUsername()) &&
                ObjectUtil.isEmpty(jdbcOssConfig.getPassword()) && ObjectUtil.isEmpty(jdbcOssConfig.getDataSourceName());
    }

    public void registerJdbcOssClient(String jdbcOssClientBeanName, JdbcOssConfig jdbcOssConfig, ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        if (ObjectUtil.isNotEmpty(jdbcOssConfig.getDataSourceName())) {
            consumer.accept(jdbcOssClientBeanName, JdbcOssClient.class, buildBeanProMap(jdbcOssConfig, jdbcOssOperation(SpringUtil.getBean(jdbcOssConfig.getDataSourceName(), DataSource.class))));
        } else {
            consumer.accept(jdbcOssClientBeanName, JdbcOssClient.class, buildBeanProMap(jdbcOssConfig, jdbcOssOperation(dataSource(jdbcOssConfig))));
        }
    }

    public StandardOssClient jdbcOssClient(DataSource dataSource, JdbcOssConfig jdbcOssConfig) throws Exception {
        return new JdbcOssClient(jdbcOssConfig, jdbcOssOperation(dataSource));
    }

    public StandardOssClient jdbcOssClient(JdbcOssConfig jdbcOssConfig) {
        return new JdbcOssClient(jdbcOssConfig, jdbcOssOperation(dataSource(jdbcOssConfig)));
    }

    public Map<String, Object> buildBeanProMap(JdbcOssConfig jdbcOssConfig, JdbcOssOperation jdbcOssOperation) {
        Map<String, Object> beanProMap = new HashMap<>();
        beanProMap.put("jdbcOssConfig", jdbcOssConfig);
        beanProMap.put("jdbcOssOperation", jdbcOssOperation);
        return beanProMap;
    }

    public DataSource dataSource(JdbcOssConfig jdbcOssConfig) {
        Class<? extends DataSource> type = jdbcOssConfig.getType();
        if (ObjectUtil.isEmpty(type)) {
            type = HikariDataSource.class;
        }
        return DataSourceBuilder.create()
                .type(type)
                .driverClassName(jdbcOssConfig.getDriver())
                .url(jdbcOssConfig.getUrl())
                .username(jdbcOssConfig.getUsername())
                .password(jdbcOssConfig.getPassword())
                .build();
    }

    public JdbcOssOperation jdbcOssOperation(DataSource dataSource) {
        JdbcOssOperationFactoryBean jdbcOssOperationFactory = new JdbcOssOperationFactoryBean();
        jdbcOssOperationFactory.setDataSource(dataSource);
        return jdbcOssOperationFactory.getObject();
    }
}
