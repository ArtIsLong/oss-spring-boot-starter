package io.github.artislong.core.ali;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ali.model.AliOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author 陈敏
 * @version AliOssConfiguration.java, v 1.1 2021/11/16 15:25 chenmin Exp $
 * Created on 2021/11/16
 */
@Configuration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({AliOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.ALI + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class AliOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "aliOssClient";

    @Autowired
    private AliOssProperties aliOssProperties;

    @Bean
    public void init() {

        List<AliOssConfig> aliOssConfigs = aliOssProperties.getOssConfigs();
        if (aliOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, aliOssClient(ossClient(aliOssProperties), aliOssProperties));
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
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), aliOssClient(ossClient(aliOssConfig), aliOssConfig));
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
