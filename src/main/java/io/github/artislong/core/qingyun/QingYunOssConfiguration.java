package io.github.artislong.core.qingyun;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.aliyun.oss.OSSClient;
import com.qingstor.sdk.config.EnvContext;
import com.qingstor.sdk.service.Bucket;
import com.qingstor.sdk.service.QingStor;
import io.github.artislong.OssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.qingyun.model.QingYunOssClientConfig;
import io.github.artislong.core.qingyun.model.QingYunOssConfig;
import io.github.artislong.function.ThreeConsumer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
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
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.QINGYUN + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class QingYunOssConfiguration extends OssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "qingYunOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        QingYunOssProperties qingYunOssProperties = getOssProperties(QingYunOssProperties.class, OssConstant.OssType.QINGYUN);
        Map<String, QingYunOssConfig> ossConfigMap = qingYunOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, QingYunOssClient.class, buildBeanProMap(qingYunOssProperties));
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
                consumer.accept(name, QingYunOssClient.class, buildBeanProMap(ossConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(QingYunOssConfig qingYunOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        QingStor qingStor = qingStor(qingYunOssConfig);
        Bucket bucket = qingStor.getBucket(qingYunOssConfig.getBucketName(), qingYunOssConfig.getZone());
        beanProMap.put("qingStor", qingStor);
        beanProMap.put("bucketClient", bucket);
        beanProMap.put("qingYunOssConfig", qingYunOssConfig);
        return beanProMap;
    }

    public QingStor qingStor(QingYunOssConfig qingYunOssConfig) {
        EnvContext env = new EnvContext(qingYunOssConfig.getAccessKey(), qingYunOssConfig.getAccessSecret());
        QingYunOssClientConfig clientConfig = Optional.ofNullable(qingYunOssConfig.getClientConfig()).orElse(new QingYunOssClientConfig());
        env.setHttpConfig(clientConfig.toClientConfig());
        String endpoint = qingYunOssConfig.getEndpoint();
        if (ObjectUtil.isNotEmpty(endpoint)) {
            env.setEndpoint(endpoint);
        }
        env.setCnameSupport(clientConfig.getCnameSupport());
        String additionalUserAgent = clientConfig.getAdditionalUserAgent();
        if (ObjectUtil.isNotEmpty(additionalUserAgent)) {
            env.setAdditionalUserAgent(additionalUserAgent);
        }
        env.setVirtualHostEnabled(clientConfig.getVirtualHostEnabled());
        return new QingStor(env);
    }
}
