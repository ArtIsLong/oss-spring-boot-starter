package io.github.artislong.core.jinshan;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ksyun.ks3.service.Ks3;
import com.ksyun.ks3.service.Ks3Client;
import com.ksyun.ks3.service.Ks3ClientConfig;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jinshan.model.JinShanOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author 陈敏
 * @version JinShanOssConfiguration.java, v 1.1 2022/3/3 22:10 chenmin Exp $
 * Created on 2022/3/3
 */
@Configuration
@ConditionalOnClass(Ks3.class)
@EnableConfigurationProperties({JinShanOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.JINSHAN + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class JinShanOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "jinShanOssClient";

    @Autowired
    private JinShanOssProperties jinShanOssProperties;

    @Bean
    public void jinShanOssClient() {
        Map<String, JinShanOssConfig> ossConfigMap = jinShanOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, jinShanOssClient(jinShanOssProperties));
        } else {
            String endpoint = jinShanOssProperties.getEndpoint();
            String accessKeyId = jinShanOssProperties.getAccessKeyId();
            String accessKeySecret = jinShanOssProperties.getAccessKeySecret();
            ossConfigMap.forEach((name, jinShanOssConfig) -> {
                if (ObjectUtil.isEmpty(jinShanOssConfig.getEndpoint())) {
                    jinShanOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(jinShanOssConfig.getAccessKeyId())) {
                    jinShanOssConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(jinShanOssConfig.getAccessKeySecret())) {
                    jinShanOssConfig.setAccessKeySecret(accessKeySecret);
                }
                SpringUtil.registerBean(name, jinShanOssClient(jinShanOssConfig));
            });
        }
    }

    public StandardOssClient jinShanOssClient(JinShanOssConfig jinShanOssConfig) {
        return new JinShanOssClient(ks3(jinShanOssConfig), jinShanOssConfig);
    }

    public Ks3 ks3(JinShanOssConfig ossConfig) {
        Ks3ClientConfig config = new Ks3ClientConfig();
        config.setEndpoint(ossConfig.getEndpoint());
        config.setDomainMode(ossConfig.isDomainMode());
        config.setProtocol(ossConfig.getProtocol());
        config.setPathStyleAccess(ossConfig.isPathStyleAccess());
        return new Ks3Client(ossConfig.getAccessKeyId(),ossConfig.getAccessKeySecret(),config);
    }
}
