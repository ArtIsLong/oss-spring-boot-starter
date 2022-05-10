package io.github.artislong.core.tencent;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.tencent.model.TencentOssClientConfig;
import io.github.artislong.core.tencent.model.TencentOssConfig;
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
 * @version TencentConfiguration.java, v 1.1 2021/11/24 15:22 chenmin Exp $
 * Created on 2021/11/24
 */
@SpringBootConfiguration
@ConditionalOnClass(COSClient.class)
@EnableConfigurationProperties({TencentOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.TENCENT + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class TencentOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "tencentOssClient";

    @Autowired
    private TencentOssProperties tencentOssProperties;

    @Bean
    public StandardOssClient tencentOssClient() {
        Map<String, TencentOssConfig> tencentOssConfigMap = tencentOssProperties.getOssConfig();
        if (tencentOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, tencentOssClient(tencentOssProperties));
        } else {
            String secretId = tencentOssProperties.getSecretId();
            String secretKey = tencentOssProperties.getSecretKey();
            TencentOssClientConfig clientConfig = tencentOssProperties.getClientConfig();
            tencentOssConfigMap.forEach((name, tencentOssConfig) -> {
                if (ObjectUtil.isEmpty(tencentOssConfig.getSecretId())) {
                    tencentOssConfig.setSecretId(secretId);
                }
                if (ObjectUtil.isEmpty(tencentOssConfig.getSecretKey())) {
                    tencentOssConfig.setSecretKey(secretKey);
                }
                if (ObjectUtil.isEmpty(tencentOssConfig.getClientConfig())) {
                    tencentOssConfig.setClientConfig(clientConfig);
                }
                SpringUtil.registerBean(name, tencentOssClient(tencentOssConfig));
            });
        }
        return null;
    }

    private StandardOssClient tencentOssClient(TencentOssConfig tencentOssConfig) {
        TencentOssClientConfig clientConfig = Optional.ofNullable(tencentOssConfig.getClientConfig()).orElse(new TencentOssClientConfig());
        COSCredentials cosCredentials = cosCredentials(tencentOssConfig);
        COSClient cosClient = cosClient(cosCredentials, clientConfig.toClientConfig());
        return tencentOssClient(cosClient, tencentOssConfig);
    }

    public StandardOssClient tencentOssClient(COSClient cosClient, TencentOssConfig tencentOssConfig) {
        return new TencentOssClient(cosClient, tencentOssConfig);
    }

    public COSCredentials cosCredentials(TencentOssConfig tencentOssConfig) {
        return new BasicCOSCredentials(tencentOssConfig.getSecretId(), tencentOssConfig.getSecretKey());
    }

    public COSClient cosClient(COSCredentials cred, ClientConfig clientConfig) {
        return new COSClient(cred, clientConfig);
    }
}
