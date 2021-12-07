package io.github.artislong;

import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.local.LocalOssClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author 陈敏
 * @version OssAutoConfiguration.java, v 1.1 2021/11/5 11:05 chenmin Exp $
 * Created on 2021/11/5
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Autowired
    private OssProperties ossProperties;

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.LOCAL)
    public StandardOssClient localOssClient() {
        return new LocalOssClient(ossProperties);
    }

}
