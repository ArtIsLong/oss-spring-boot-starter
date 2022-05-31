package io.github.artislong.core.jinshan;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ksyun.ks3.http.Region;
import com.ksyun.ks3.service.Ks3;
import com.ksyun.ks3.service.Ks3Client;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jinshan.model.JinShanOssClientConfig;
import io.github.artislong.core.jinshan.model.JinShanOssConfig;
import io.github.artislong.core.jinshan.model.Ks3ClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.Optional;

/**
 * @author 陈敏
 * @version JinShanOssConfiguration.java, v 1.1 2022/3/3 22:10 chenmin Exp $
 * Created on 2022/3/3
 */
@SpringBootConfiguration
@ConditionalOnClass(Ks3.class)
@EnableConfigurationProperties({JinShanOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.JINSHAN + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class JinShanOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "jinShanOssClient";

    @Autowired
    private JinShanOssProperties jinShanOssProperties;

    @Bean
    public StandardOssClient jinShanOssClient() {
        Map<String, JinShanOssConfig> ossConfigMap = jinShanOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, jinShanOssClient(jinShanOssProperties));
        } else {
            String accessKeyId = jinShanOssProperties.getAccessKeyId();
            String accessKeySecret = jinShanOssProperties.getAccessKeySecret();
            String endpoint = jinShanOssProperties.getEndpoint();
            Region region = jinShanOssProperties.getRegion();
            String securityToken = jinShanOssProperties.getSecurityToken();
            JinShanOssClientConfig clientConfig = jinShanOssProperties.getClientConfig();
            ossConfigMap.forEach((name, jinShanOssConfig) -> {
                if (ObjectUtil.isEmpty(jinShanOssConfig.getAccessKeyId())) {
                    jinShanOssConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(jinShanOssConfig.getAccessKeySecret())) {
                    jinShanOssConfig.setAccessKeySecret(accessKeySecret);
                }
                if (ObjectUtil.isEmpty(jinShanOssConfig.getEndpoint())) {
                    jinShanOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(jinShanOssConfig.getRegion())) {
                    jinShanOssConfig.setRegion(region);
                }
                if (ObjectUtil.isEmpty(jinShanOssConfig.getSecurityToken())) {
                    jinShanOssConfig.setSecurityToken(securityToken);
                }
                if (ObjectUtil.isEmpty(jinShanOssConfig.getClientConfig())) {
                    jinShanOssConfig.setClientConfig(clientConfig);
                }
                SpringUtil.registerBean(name, jinShanOssClient(jinShanOssConfig));
            });
        }
        return null;
    }

    public StandardOssClient jinShanOssClient(JinShanOssConfig jinShanOssConfig) {
        return new JinShanOssClient(ks3(jinShanOssConfig), jinShanOssConfig);
    }

    public Ks3 ks3(JinShanOssConfig ossConfig) {
        JinShanOssClientConfig jinShanOssClientConfig = Optional.ofNullable(ossConfig.getClientConfig()).orElse(new JinShanOssClientConfig());

        Ks3ClientConfig clientConfig = jinShanOssClientConfig.toClientConfig();
        clientConfig.setHttpClientConfig(jinShanOssClientConfig.toHttpClientConfig());
        clientConfig.setEndpoint(ossConfig.getEndpoint());
        clientConfig.setRegion(ossConfig.getRegion());

        if (ObjectUtil.isNotEmpty(ossConfig.getSecurityToken())) {
            return new Ks3Client(ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret(), ossConfig.getSecurityToken(), clientConfig);
        } else {
            return new Ks3Client(ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret(), clientConfig);
        }
    }
}
