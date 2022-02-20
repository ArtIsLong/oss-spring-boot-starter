package io.github.artislong.core.tencent;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.tencent.model.TencentOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author 陈敏
 * @version TencentConfiguration.java, v 1.1 2021/11/24 15:22 chenmin Exp $
 * Created on 2021/11/24
 */
@Configuration
@ConditionalOnClass(COSClient.class)
@EnableConfigurationProperties({TencentOssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "tencent", havingValue = "true")
public class TencentOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "tencentOssClient";

    @Autowired
    private TencentOssProperties tencentOssProperties;

    @PostConstruct
    public void init() {
        List<TencentOssConfig> tencentOssConfigs = tencentOssProperties.getTencentOssConfigs();
        if (tencentOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, build(tencentOssProperties));
        } else {
            String region = tencentOssProperties.getRegion();
            String secretId = tencentOssProperties.getSecretId();
            String secretKey = tencentOssProperties.getSecretKey();
            for (int i = 0; i < tencentOssConfigs.size(); i++) {
                TencentOssConfig tencentOssConfig = tencentOssConfigs.get(i);
                if (ObjectUtil.isEmpty(tencentOssConfig.getRegion())) {
                    tencentOssConfig.setRegion(region);
                }
                if (ObjectUtil.isEmpty(tencentOssConfig.getSecretId())) {
                    tencentOssConfig.setSecretId(secretId);
                }
                if (ObjectUtil.isEmpty(tencentOssConfig.getSecretKey())) {
                    tencentOssConfig.setSecretKey(secretKey);
                }
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), build(tencentOssConfig));
            }
        }
    }

    private StandardOssClient build(TencentOssConfig tencentOssConfig) {
        Region region = region(tencentOssConfig);
        ClientConfig clientConfig = config(region);
        COSCredentials cosCredentials = cosCredentials(tencentOssConfig);
        COSClient cosClient = cosClient(cosCredentials, clientConfig);
        return tencentOssClient(cosClient, tencentOssConfig);
    }

    public StandardOssClient tencentOssClient(COSClient cosClient, TencentOssConfig tencentOssConfig) {
        return new TencentOssClient(cosClient, tencentOssConfig);
    }

    public COSCredentials cosCredentials(TencentOssConfig tencentOssConfig) {
        return new BasicCOSCredentials(tencentOssConfig.getSecretId(), tencentOssConfig.getSecretKey());
    }

    public Region region(TencentOssConfig tencentOssConfig) {
        return new Region(tencentOssConfig.getRegion());
    }

    public ClientConfig config(Region region) {
        return new ClientConfig(region);
    }

    public COSClient cosClient(COSCredentials cred, ClientConfig clientConfig) {
        return new COSClient(cred, clientConfig);
    }
}
