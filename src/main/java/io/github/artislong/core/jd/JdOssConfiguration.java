package io.github.artislong.core.jd;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import io.github.artislong.OssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jd.model.JdOssClientConfig;
import io.github.artislong.core.jd.model.JdOssConfig;
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
 * @version JdOssConfiguration.java, v 1.1 2021/11/25 10:44 chenmin Exp $
 * Created on 2021/11/25
 */
@SpringBootConfiguration
@ConditionalOnClass(AmazonS3.class)
@EnableConfigurationProperties({JdOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.JD + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class JdOssConfiguration extends OssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "jdOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        JdOssProperties jdOssProperties = getOssProperties(JdOssProperties.class, OssConstant.OssType.JD);
        Map<String, JdOssConfig> jdOssConfigMap = jdOssProperties.getOssConfig();
        if (jdOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, JdOssClient.class, buildBeanProMap(jdOssProperties));
        } else {
            String endpoint = jdOssProperties.getEndpoint();
            String accessKey = jdOssProperties.getAccessKey();
            String secretKey = jdOssProperties.getSecretKey();
            String region = jdOssProperties.getRegion();
            JdOssClientConfig clientConfig = jdOssProperties.getClientConfig();
            jdOssConfigMap.forEach((name, jdOssConfig) -> {
                if (ObjectUtil.isEmpty(jdOssConfig.getEndpoint())) {
                    jdOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(jdOssConfig.getAccessKey())) {
                    jdOssConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(jdOssConfig.getSecretKey())) {
                    jdOssConfig.setSecretKey(secretKey);
                }
                if (ObjectUtil.isEmpty(jdOssConfig.getRegion())) {
                    jdOssConfig.setRegion(region);
                }
                if (ObjectUtil.isEmpty(jdOssConfig.getClientConfig())) {
                    jdOssConfig.setClientConfig(clientConfig);
                }
                consumer.accept(name, JdOssClient.class, buildBeanProMap(jdOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(JdOssConfig jdOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        JdOssClientConfig clientConfig = Optional.ofNullable(jdOssConfig.getClientConfig()).orElse(new JdOssClientConfig());
        AwsClientBuilder.EndpointConfiguration endpointConfig = endpointConfig(jdOssConfig);
        AWSCredentials awsCredentials = awsCredentials(jdOssConfig);
        AWSCredentialsProvider awsCredentialsProvider = awsCredentialsProvider(awsCredentials);
        AmazonS3 amazonS3 = amazonS3(endpointConfig, clientConfig.toClientConfig(), awsCredentialsProvider);
        TransferManager transferManager = transferManager(amazonS3);
        beanProMap.put("amazonS3", amazonS3);
        beanProMap.put("transferManager", transferManager);
        beanProMap.put("jdOssConfig", jdOssConfig);
        return beanProMap;
    }

    public AwsClientBuilder.EndpointConfiguration endpointConfig(JdOssConfig jdOssConfig) {
        return new AwsClientBuilder.EndpointConfiguration(jdOssConfig.getEndpoint(), jdOssConfig.getRegion());
    }

    public AWSCredentials awsCredentials(JdOssConfig jdOssConfig) {
        return new BasicAWSCredentials(jdOssConfig.getAccessKey(), jdOssConfig.getSecretKey());
    }

    public AWSCredentialsProvider awsCredentialsProvider(AWSCredentials awsCredentials) {
        return new AWSStaticCredentialsProvider(awsCredentials);
    }

    public AmazonS3 amazonS3(AwsClientBuilder.EndpointConfiguration endpointConfig, ClientConfiguration clientConfig,
                             AWSCredentialsProvider awsCredentialsProvider) {
        return AmazonS3Client.builder()
                .withEndpointConfiguration(endpointConfig)
                .withClientConfiguration(clientConfig)
                .withCredentials(awsCredentialsProvider)
                .disableChunkedEncoding()
                .build();
    }

    public TransferManager transferManager(AmazonS3 amazonS3) {
        return TransferManagerBuilder.standard()
                .withS3Client(amazonS3)
                .build();
    }
}
