package io.github.artislong.core.tencent;

import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version TencentConfiguration.java, v 1.1 2021/11/24 15:22 chenmin Exp $
 * Created on 2021/11/24
 */
@Configuration
@ConditionalOnClass(COSClient.class)
@EnableConfigurationProperties({TencentOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.TENCENT)
public class TencentOssConfiguration {

    @Autowired
    private TencentOssProperties tencentOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient tencentOssClient(COSClient cosClient) {
        return new TencentOssClient(cosClient, ossProperties, tencentOssProperties);
    }

    @Bean
    public COSCredentials cosCredentials() {
        return new BasicCOSCredentials(tencentOssProperties.getSecretId(), tencentOssProperties.getSecretKey());
    }

    @Bean
    public Region region() {
        return new Region(tencentOssProperties.getRegion());
    }

    @Bean
    public ClientConfig config(Region region) {
        return new ClientConfig(region);
    }

    @Bean
    public COSClient cosClient(COSCredentials cred, ClientConfig clientConfig) {
        return new COSClient(cred, clientConfig);
    }
}
