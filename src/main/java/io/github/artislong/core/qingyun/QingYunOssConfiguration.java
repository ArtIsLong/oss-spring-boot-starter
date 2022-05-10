package io.github.artislong.core.qingyun;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSSClient;
import com.qingstor.sdk.config.EnvContext;
import com.qingstor.sdk.service.Bucket;
import com.qingstor.sdk.service.QingStor;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.qingyun.model.QingYunOssClientConfig;
import io.github.artislong.core.qingyun.model.QingYunOssConfig;
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
 * @version QingYunOssConfiguration.java, v 1.0 2022/3/10 23:43 chenmin Exp $
 * Created on 2022/3/10
 */
@SpringBootConfiguration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({QingYunOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.QINGYUN + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class QingYunOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "qingYunOssClient";

    @Autowired
    private QingYunOssProperties qingYunOssProperties;

    @Bean
    public StandardOssClient qingYunOssClient() {
        Map<String, QingYunOssConfig> ossConfigMap = qingYunOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, qingYunOssClient(qingYunOssProperties));
        } else {
            String endpoint = qingYunOssProperties.getEndpoint();
            String accessKey = qingYunOssProperties.getAccessKey();
            String accessSecret = qingYunOssProperties.getAccessSecret();
            String zone = qingYunOssProperties.getZone();
            QingYunOssClientConfig clientConfig = qingYunOssProperties.getClientConfig();
            ossConfigMap.forEach((name, ossConfig) -> {
                if (ObjectUtil.isEmpty(ossConfig.getEndpoint())) {
                    ossConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(ossConfig.getAccessKey())) {
                    ossConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(ossConfig.getAccessSecret())) {
                    ossConfig.setAccessSecret(accessSecret);
                }
                if (ObjectUtil.isEmpty(ossConfig.getZone())) {
                    ossConfig.setZone(zone);
                }
                if (ObjectUtil.isEmpty(ossConfig.getClientConfig())) {
                    ossConfig.setClientConfig(clientConfig);
                }
                SpringUtil.registerBean(name, qingYunOssClient(ossConfig));
            });
        }
        return null;
    }

    public StandardOssClient qingYunOssClient(QingYunOssConfig qingYunOssConfig) {
        QingStor qingStor = qingStor(qingYunOssConfig);
        Bucket bucket = qingStor.getBucket(qingYunOssConfig.getBucketName(), qingYunOssConfig.getZone());
        return new QingYunOssClient(qingStor, bucket, qingYunOssConfig);
    }

    public QingStor qingStor(QingYunOssConfig qingYunOssConfig) {
        EnvContext env = new EnvContext(qingYunOssConfig.getAccessKey(), qingYunOssConfig.getAccessSecret());
        QingYunOssClientConfig clientConfig = Optional.ofNullable(qingYunOssConfig.getClientConfig()).orElse(new QingYunOssClientConfig());
        env.setHttpConfig(clientConfig.toClientConfig());
        String endpoint = qingYunOssConfig.getEndpoint();
        if (ObjectUtil.isNotEmpty(endpoint)) {
            env.setEndpoint(endpoint);
        }
        env.setCnameSupport(clientConfig.isCnameSupport());
        String additionalUserAgent = clientConfig.getAdditionalUserAgent();
        if (ObjectUtil.isNotEmpty(additionalUserAgent)) {
            env.setAdditionalUserAgent(additionalUserAgent);
        }
        env.setVirtualHostEnabled(clientConfig.isVirtualHostEnabled());
        return new QingStor(env);
    }
}
