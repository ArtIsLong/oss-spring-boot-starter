package io.github.artislong.core.inspur;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.inspurcloud.oss.client.OSSClient;
import com.inspurcloud.oss.client.impl.OSSClientImpl;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.inspur.model.InspurOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * @author 陈敏
 * @version InspurOssConfiguration.java, v 1.0 2022/5/17 13:10 chenmin Exp $
 * Created on 2022/5/17
 */
@SpringBootConfiguration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({InspurOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.INSPUR + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class InspurOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "inspurOssClient";

    @Autowired
    private InspurOssProperties inspurOssProperties;

    @Bean
    public StandardOssClient inspurOssClient() {
        Map<String, InspurOssConfig> ossConfigMap = inspurOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, inspurOssClient(inspurOssProperties));
        } else {
            String endpoint = inspurOssProperties.getEndpoint();
            String accessKey = inspurOssProperties.getAccessKey();
            String secretKey = inspurOssProperties.getSecretKey();
            ossConfigMap.forEach((name, inspurOssConfig) -> {
                if (ObjectUtil.isEmpty(inspurOssConfig.getEndpoint())) {
                    inspurOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(inspurOssConfig.getAccessKey())) {
                    inspurOssConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(inspurOssConfig.getSecretKey())) {
                    inspurOssConfig.setSecretKey(secretKey);
                }
                SpringUtil.registerBean(name, inspurOssConfig);
            });
        }
        return null;
    }

    public StandardOssClient inspurOssClient(InspurOssConfig ossConfig) {
        return new InspurOssClient(ossClient(ossConfig), ossConfig);
    }

    public OSSClient ossClient(InspurOssConfig ossConfig) {
        return new OSSClientImpl(ossConfig.getEndpoint(), ossConfig.getAccessKey(), ossConfig.getSecretKey());
    }
}
