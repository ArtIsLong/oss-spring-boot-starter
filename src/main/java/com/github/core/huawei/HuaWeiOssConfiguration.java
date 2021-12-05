package com.github.core.huawei;

import com.github.OssProperties;
import com.github.constant.OssConstant;
import com.github.core.StandardOssClient;
import com.obs.services.ObsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version HuaWeiOssConfiguration.java, v 1.1 2021/11/25 9:58 chenmin Exp $
 * Created on 2021/11/25
 */
@Configuration
@ConditionalOnClass(ObsClient.class)
@EnableConfigurationProperties({HuaWeiOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.HUAWEI)
public class HuaWeiOssConfiguration {

    @Autowired
    private HuaWeiOssProperties huaWeiOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient huaWeiOssClient(ObsClient obsClient) {
        return new HuaWeiOssClient(obsClient, huaWeiOssProperties, ossProperties);
    }

    @Bean
    public ObsClient obsClient() {
        return new ObsClient(huaWeiOssProperties.getAccessKey(), huaWeiOssProperties.getSecretKey(), huaWeiOssProperties.getEndpoint());
    }
}
