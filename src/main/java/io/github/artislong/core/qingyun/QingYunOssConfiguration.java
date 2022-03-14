package io.github.artislong.core.qingyun;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSSClient;
import com.qingstor.sdk.config.EnvContext;
import com.qingstor.sdk.service.Bucket;
import com.qingstor.sdk.service.QingStor;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.qingyun.model.QingYunOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author 陈敏
 * @version QingYunOssConfiguration.java, v 1.0 2022/3/10 23:43 chenmin Exp $
 * Created on 2022/3/10
 */
@Configuration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({QingYunOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.QINGYUN + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class QingYunOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "qingYunOssClient";

    @Autowired
    private QingYunOssProperties qingYunOssProperties;

    @Bean
    public void qingYunOssClient() {
        Map<String, QingYunOssConfig> ossConfigMap = qingYunOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, qingYunOssClient(qingYunOssProperties));
        } else {
            String accessKey = qingYunOssProperties.getAccessKey();
            String accessSecret = qingYunOssProperties.getAccessSecret();
            ossConfigMap.forEach((name, ossConfig) -> {
                if (ObjectUtil.isEmpty(ossConfig.getAccessKey())) {
                    ossConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(ossConfig.getAccessSecret())) {
                    ossConfig.setAccessSecret(accessSecret);
                }
                SpringUtil.registerBean(name, qingYunOssClient(ossConfig));
            });
        }
    }

    public StandardOssClient qingYunOssClient(QingYunOssConfig qingYunOssConfig) {
        QingStor qingStor = qingStor(qingYunOssConfig);
        Bucket bucket = qingStor.getBucket(qingYunOssConfig.getBucketName(), qingYunOssConfig.getZone());
        return new QingYunOssClient(qingStor, bucket, qingYunOssConfig);
    }

    public QingStor qingStor(QingYunOssConfig qingYunOssConfig) {
        EnvContext env = new EnvContext(qingYunOssConfig.getAccessKey(), qingYunOssConfig.getAccessSecret());
        return new QingStor(env);
    }
}
