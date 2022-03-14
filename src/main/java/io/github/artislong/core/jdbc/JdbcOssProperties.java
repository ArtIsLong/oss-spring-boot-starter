package io.github.artislong.core.jdbc;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.jdbc.model.JdbcOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version JdbcOssProperties.java, v 1.0 2022/3/11 21:36 chenmin Exp $
 * Created on 2022/3/11
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.JDBC)
public class JdbcOssProperties extends JdbcOssConfig implements InitializingBean {
    private Boolean enable = false;

    private Map<String, JdbcOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(JdbcOssConfig::init);
        }
    }
}
