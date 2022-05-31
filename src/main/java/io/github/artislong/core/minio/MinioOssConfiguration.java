package io.github.artislong.core.minio;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.minio.model.MinioOssClientConfig;
import io.github.artislong.core.minio.model.MinioOssConfig;
import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
public class MinioOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "minioOssClient";

    @Autowired
    private MinioOssProperties minioOssProperties;

    @Bean
    public StandardOssClient minioOssClient() {
        Map<String, MinioOssConfig> minioOssConfigMap = minioOssProperties.getOssConfig();
        if (minioOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, minioOssClient(minioOssProperties));
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
                SpringUtil.registerBean(name, minioOssClient(minioOssConfig));
            });
        }
        return null;
    }

    public StandardOssClient minioOssClient(MinioOssConfig minioOssConfig) {
        return new MinioOssClient(minioClient(minioOssConfig), minioOssConfig);
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
