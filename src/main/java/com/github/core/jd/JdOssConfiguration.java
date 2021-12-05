package com.github.core.jd;

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
import com.github.OssProperties;
import com.github.constant.OssConstant;
import com.github.core.StandardOssClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version JdOssConfiguration.java, v 1.1 2021/11/25 10:44 chenmin Exp $
 * Created on 2021/11/25
 */
@Configuration
@ConditionalOnClass(AmazonS3.class)
@EnableConfigurationProperties({JdOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.JD)
public class JdOssConfiguration {

    @Autowired
    private JdOssProperties jdOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient jdOssClient(AmazonS3 amazonS3, TransferManager transferManager) {
        return new JdOssClient(amazonS3, transferManager, ossProperties, jdOssProperties);
    }

    @Bean
    public ClientConfiguration clientConfig() {
        return new ClientConfiguration();
    }

    @Bean
    public AwsClientBuilder.EndpointConfiguration endpointConfig() {
        return new AwsClientBuilder.EndpointConfiguration(jdOssProperties.getEndpoint(), jdOssProperties.getRegion());
    }

    @Bean
    public AWSCredentials awsCredentials() {
        return new BasicAWSCredentials(jdOssProperties.getAccessKey(), jdOssProperties.getSecretKey());
    }

    @Bean
    public AWSCredentialsProvider awsCredentialsProvider(AWSCredentials awsCredentials) {
        return new AWSStaticCredentialsProvider(awsCredentials);
    }

    @Bean
    public AmazonS3 amazonS3(AwsClientBuilder.EndpointConfiguration endpointConfig, ClientConfiguration clientConfig,
                             AWSCredentialsProvider awsCredentialsProvider) {
        return AmazonS3Client.builder()
                .withEndpointConfiguration(endpointConfig)
                .withClientConfiguration(clientConfig)
                .withCredentials(awsCredentialsProvider)
                .disableChunkedEncoding()
                .build();
    }

    @Bean
    public TransferManager transferManager(AmazonS3 amazonS3) {
        return TransferManagerBuilder.standard()
                .withS3Client(amazonS3)
                .build();
    }
}
