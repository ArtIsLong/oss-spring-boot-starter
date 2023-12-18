package io.github.artislong.core.minio;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.artislong.OssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.minio.model.MinioOssClientConfig;
import io.github.artislong.core.minio.model.MinioOssConfig;
import io.github.artislong.function.ThreeConsumer;
import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 陈敏
 * @version MinioConfiguration.java, v 1.1 2021/11/24 15:20 chenmin Exp $
 * Created on 2021/11/24
 */
@SpringBootConfiguration
@ConditionalOnClass(MinioClient.class)
@EnableConfigurationProperties({MinioOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.MINIO + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class MinioOssConfiguration extends OssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "minioOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        MinioOssProperties minioOssProperties = getOssProperties(MinioOssProperties.class, OssConstant.OssType.MINIO);
        Map<String, MinioOssConfig> minioOssConfigMap = minioOssProperties.getOssConfig();
        if (minioOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, MinioOssClient.class, buildBeanProMap(minioOssProperties));
        } else {
            String endpoint = minioOssProperties.getEndpoint();
            String accessKey = minioOssProperties.getAccessKey();
            String secretKey = minioOssProperties.getSecretKey();
            MinioOssClientConfig clientConfig = minioOssProperties.getClientConfig();
            minioOssConfigMap.forEach((name, minioOssConfig) -> {
                if (ObjectUtil.isEmpty(minioOssConfig.getEndpoint())) {
                    minioOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(minioOssConfig.getAccessKey())) {
                    minioOssConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(minioOssConfig.getSecretKey())) {
                    minioOssConfig.setSecretKey(secretKey);
                }
                if (ObjectUtil.isEmpty(minioOssConfig.getClientConfig())) {
                    minioOssConfig.setClientConfig(clientConfig);
                }
                consumer.accept(name, MinioOssClient.class, buildBeanProMap(minioOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(MinioOssConfig minioOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        MinioClient minioClient = minioClient(minioOssConfig);
        beanProMap.put("minioClient", minioClient);
        beanProMap.put("minioOssConfig", minioOssConfig);
        return beanProMap;
    }

    public MinioClient minioClient(MinioOssConfig minioOssConfig) {
        MinioOssClientConfig clientConfig = minioOssConfig.getClientConfig();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(clientConfig.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(clientConfig.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(clientConfig.getReadTimeout(), TimeUnit.MILLISECONDS)
                .callTimeout(clientConfig.getCallTimeout(), TimeUnit.MILLISECONDS)
                .followRedirects(clientConfig.getFollowRedirects())
                .followSslRedirects(clientConfig.getFollowSslRedirects())
                .retryOnConnectionFailure(clientConfig.getRetryOnConnectionFailure())
                .pingInterval(clientConfig.getPingInterval(), TimeUnit.MILLISECONDS)
                .build();
        return MinioClient.builder()
                .endpoint(minioOssConfig.getEndpoint())
                .credentials(minioOssConfig.getAccessKey(), minioOssConfig.getSecretKey())
                .httpClient(okHttpClient)
                .build();
    }
}
