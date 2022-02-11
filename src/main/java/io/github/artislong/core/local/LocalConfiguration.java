package io.github.artislong.core.local;

import com.aliyun.oss.OSSClient;
import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version LocalConfiguration.java, v 1.1 2022/2/11 15:28 chenmin Exp $
 * Created on 2022/2/11
 */
@Configuration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({LocalProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.LOCAL)
public class LocalConfiguration {

    @Autowired
    private OssProperties ossProperties;
    @Autowired
    private LocalProperties localProperties;

    @Bean
    public StandardOssClient localOssClient() {
        return new LocalOssClient(ossProperties, localProperties);
    }
}
