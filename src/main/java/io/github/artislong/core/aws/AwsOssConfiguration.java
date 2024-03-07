package io.github.artislong.core.aws;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.OssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.aws.constant.AwsRegion;
import io.github.artislong.core.aws.model.AwsOssClientConfig;
import io.github.artislong.core.aws.model.AwsOssConfig;
import io.github.artislong.function.ThreeConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version AwsOssConfiguration.java, v 1.0 2022/4/1 18:02 chenmin Exp $
 * Created on 2022/4/1
 */
@Configuration
@ConditionalOnClass(S3Client.class)
@EnableConfigurationProperties({AwsOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssType.AWS + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class AwsOssConfiguration extends OssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "awsOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        AwsOssProperties awsOssProperties = getOssProperties(AwsOssProperties.class, OssType.AWS);
        Map<String, AwsOssConfig> ossConfigMap = awsOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, AwsOssClient.class, buildBeanProMap(awsOssProperties));
        } else {
            String accessKeyId = awsOssProperties.getAccessKeyId();
            String secretAccessKey = awsOssProperties.getSecretAccessKey();
            DefaultsMode mode = awsOssProperties.getMode();
            AwsRegion region = awsOssProperties.getRegion();
            ossConfigMap.forEach((name, ossConfig) -> {
                if (ObjectUtil.isEmpty(ossConfig.getAccessKeyId())) {
                    ossConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(ossConfig.getSecretAccessKey())) {
                    ossConfig.setSecretAccessKey(secretAccessKey);
                }
                if (ObjectUtil.isEmpty(ossConfig.getMode())) {
                    ossConfig.setMode(mode);
                }
                if (ObjectUtil.isEmpty(ossConfig.getRegion())) {
                    ossConfig.setRegion(region);
                }
                consumer.accept(name, AwsOssClient.class, buildBeanProMap(ossConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(AwsOssConfig ossConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        S3Client s3Client = s3Client(ossConfig);
        beanProMap.put("ossConfig", ossConfig);
        beanProMap.put("s3Client", s3Client);
        return beanProMap;
    }

    public S3Client s3Client(AwsOssConfig ossConfig) {
        AwsOssClientConfig clientConfig = ossConfig.getClientConfig();
        return S3Client.builder().credentialsProvider(() -> new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return ossConfig.getAccessKeyId();
                    }

                    @Override
                    public String secretAccessKey() {
                        return ossConfig.getSecretAccessKey();
                    }
                }).region(ossConfig.getRegion().getRegion())
                .serviceConfiguration(builder -> builder
                        .accelerateModeEnabled(clientConfig.getAccelerateModeEnabled())
                        .checksumValidationEnabled(clientConfig.getChecksumValidationEnabled())
                        .multiRegionEnabled(clientConfig.getMultiRegionEnabled())
                        .chunkedEncodingEnabled(clientConfig.getChunkedEncodingEnabled())
                        .pathStyleAccessEnabled(clientConfig.getPathStyleAccessEnabled())
                        .useArnRegionEnabled(clientConfig.getUseArnRegionEnabled())
                )
                .fipsEnabled(clientConfig.getFipsEnabled())
                .dualstackEnabled(clientConfig.getDualstackEnabled()).build();
    }
}
