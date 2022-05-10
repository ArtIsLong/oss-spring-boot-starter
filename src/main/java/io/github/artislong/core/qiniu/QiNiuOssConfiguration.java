package io.github.artislong.core.qiniu;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.qiniu.model.QiNiuOssClientConfig;
import io.github.artislong.core.qiniu.model.QiNiuOssConfig;
import io.github.artislong.model.SliceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.Optional;

/**
 * @author 陈敏
 * @version QiNiuOssConfiguration.java, v 1.1 2021/11/16 15:31 chenmin Exp $
 * Created on 2021/11/16
 */
@SpringBootConfiguration
@EnableConfigurationProperties({QiNiuOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.QINIU + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class QiNiuOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "qiNiuOssClient";

    @Autowired
    private QiNiuOssProperties qiNiuOssProperties;

    @Bean
    public StandardOssClient qiNiuOssClient() {
        Map<String, QiNiuOssConfig> qiNiuOssConfigMap = qiNiuOssProperties.getOssConfig();
        if (qiNiuOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, qiNiuOssClient(qiNiuOssProperties));
        } else {
            String accessKey = qiNiuOssProperties.getAccessKey();
            String secretKey = qiNiuOssProperties.getSecretKey();
            qiNiuOssConfigMap.forEach((name, qiNiuOssConfig) -> {
                if (ObjectUtil.isEmpty(qiNiuOssConfig.getAccessKey())) {
                    qiNiuOssConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(qiNiuOssConfig.getSecretKey())) {
                    qiNiuOssConfig.setSecretKey(secretKey);
                }
                SpringUtil.registerBean(name, qiNiuOssClient(qiNiuOssConfig));
            });
        }
        return null;
    }

    private StandardOssClient qiNiuOssClient(QiNiuOssConfig qiNiuOssConfig) {
        Auth auth = auth(qiNiuOssConfig);
        Configuration configuration = configuration(qiNiuOssConfig);
        UploadManager uploadManager = uploadManager(configuration);
        BucketManager bucketManager = bucketManager(auth, configuration);
        return qiNiuOssClient(auth, uploadManager, bucketManager, qiNiuOssConfig, configuration);
    }

    public StandardOssClient qiNiuOssClient(Auth auth, UploadManager uploadManager, BucketManager bucketManager,
                                            QiNiuOssConfig qiNiuOssConfig, Configuration configuration) {
        return new QiNiuOssClient(auth, uploadManager, bucketManager, qiNiuOssConfig, configuration);
    }

    public Auth auth(QiNiuOssConfig qiNiuOssConfig) {
        return Auth.create(qiNiuOssConfig.getAccessKey(), qiNiuOssConfig.getSecretKey());
    }

    public UploadManager uploadManager(Configuration configuration) {
        return new UploadManager(configuration);
    }

    public BucketManager bucketManager(Auth auth, Configuration configuration) {
        return new BucketManager(auth, configuration);
    }

    public Configuration configuration(QiNiuOssConfig qiNiuOssConfig) {
        Configuration configuration = Optional.ofNullable(qiNiuOssConfig.getClientConfig()).orElse(new QiNiuOssClientConfig()).toClientConfig();
        SliceConfig sliceConfig = qiNiuOssConfig.getSliceConfig();
        configuration.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        configuration.resumableUploadMaxConcurrentTaskCount = sliceConfig.getTaskNum();
        configuration.resumableUploadAPIV2BlockSize = sliceConfig.getPartSize().intValue();
        return configuration;
    }


}
