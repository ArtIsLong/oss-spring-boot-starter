package io.github.artislong.core.minio;

import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version MinioConfiguration.java, v 1.1 2021/11/24 15:20 chenmin Exp $
 * Created on 2021/11/24
 */
@Configuration
@ConditionalOnClass(MinioClient.class)
@EnableConfigurationProperties({MinioOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.MINIO)
public class MinioOssConfiguration {

    @Autowired
    private MinioOssProperties minioOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient minioOssClient(MinioClient minioClient) {
        return new MinioOssClient(minioClient, ossProperties, minioOssProperties);
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioOssProperties.getEndpoint())
                .credentials(minioOssProperties.getAccessKey(), minioOssProperties.getSecretKey())
                .build();
    }
}
