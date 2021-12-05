package com.github.core.qiniu;

import com.github.OssProperties;
import com.github.constant.OssConstant;
import com.github.core.StandardOssClient;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version QiNiuOssConfiguration.java, v 1.1 2021/11/16 15:31 chenmin Exp $
 * Created on 2021/11/16
 */
@Configuration
@EnableConfigurationProperties({QiNiuOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.QINIU)
public class QiNiuOssConfiguration {

    @Autowired
    private QiNiuOssProperties qiNiuOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient qiNiuOssClient(Auth auth, UploadManager uploadManager, BucketManager bucketManager) {
        return new QiNiuOssClient(auth, uploadManager, bucketManager, ossProperties, qiNiuOssProperties);
    }

    @Bean
    public Auth auth() {
        return Auth.create(qiNiuOssProperties.getAccessKey(), qiNiuOssProperties.getSecretKey());
    }

    @Bean
    public UploadManager uploadManager(com.qiniu.storage.Configuration configuration) {
        return new UploadManager(configuration);
    }

    @Bean
    public BucketManager bucketManager(Auth auth, com.qiniu.storage.Configuration configuration) {
        return new BucketManager(auth, configuration);
    }

    @Bean
    public com.qiniu.storage.Configuration configuration() {
        return new com.qiniu.storage.Configuration(qiNiuOssProperties.getRegion().buildRegion());
    }


}
