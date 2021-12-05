package com.github.core.baidu;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.github.OssProperties;
import com.github.constant.OssConstant;
import com.github.core.StandardOssClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version BaiduConfiguration.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@Configuration
@ConditionalOnClass(BosClient.class)
@EnableConfigurationProperties({BaiduOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.BAIDU)
public class BaiduOssConfiguration {

    @Autowired
    private BaiduOssProperties baiduOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient baiduOssClient(BosClient bosClient) {
        return new BaiduOssClient(bosClient, ossProperties, baiduOssProperties);
    }

    @Bean
    public BosClientConfiguration bosClientConfiguration() {
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(baiduOssProperties.getAccessKeyId(), baiduOssProperties.getSecretAccessKey()));
        config.setEndpoint(baiduOssProperties.getEndPoint());
        return config;
    }

    @Bean
    public BosClient bosClient(BosClientConfiguration config) {
        return new BosClient(config);
    }

}
