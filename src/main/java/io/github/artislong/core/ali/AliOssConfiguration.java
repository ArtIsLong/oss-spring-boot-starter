package io.github.artislong.core.ali;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ali.model.AliOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author 陈敏
 * @version AliOssConfiguration.java, v 1.1 2021/11/16 15:25 chenmin Exp $
 * Created on 2021/11/16
 */
@Configuration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({AliOssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "ali", havingValue = "true")
public class AliOssConfiguration {

    @Autowired
    private AliOssProperties aliOssProperties;

    @PostConstruct
    public void init() {
        final String defaultBeanName = "aliOssClient";

        List<AliOssConfig> aliOssConfigs = aliOssProperties.getAliOssConfigs();
        if (aliOssConfigs.isEmpty()) {
            SpringUtil.registerBean(defaultBeanName, aliOssClient(ossClient(aliOssProperties), aliOssProperties));
        } else {
            String endpoint = aliOssProperties.getEndpoint();
            String accessKeyId = aliOssProperties.getAccessKeyId();
            String accessKeySecret = aliOssProperties.getAccessKeySecret();
            for (int i = 0; i < aliOssConfigs.size(); i++) {
                AliOssConfig aliOssConfig = aliOssConfigs.get(i);
                if (ObjectUtil.isEmpty(aliOssConfig.getEndpoint())) {
                    aliOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(aliOssConfig.getAccessKeyId())) {
                    aliOssConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(aliOssConfig.getAccessKeySecret())) {
                    aliOssConfig.setAccessKeySecret(accessKeySecret);
                }
                SpringUtil.registerBean(defaultBeanName + (i + 1), aliOssClient(ossClient(aliOssConfig), aliOssConfig));
            }
        }
    }

    public StandardOssClient aliOssClient(OSS oss, AliOssConfig aliOssConfig) {
        return new AliOssClient(oss, aliOssConfig);
    }

    public OSS ossClient(AliOssConfig aliOssConfig) {
        return new OSSClientBuilder().build(aliOssConfig.getEndpoint(),
                aliOssConfig.getAccessKeyId(),
                aliOssConfig.getAccessKeySecret());
    }

}
