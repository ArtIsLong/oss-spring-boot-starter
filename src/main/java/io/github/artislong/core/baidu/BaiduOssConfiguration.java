package io.github.artislong.core.baidu;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.baidu.model.BaiduOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author 陈敏
 * @version BaiduConfiguration.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@Configuration
@ConditionalOnClass(BosClient.class)
@EnableConfigurationProperties({BaiduOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.BAIDU + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class BaiduOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "baiduOssClient";

    @Autowired
    private BaiduOssProperties baiduOssProperties;

    @Bean
    public StandardOssClient baiduOssClient() {
        Map<String, BaiduOssConfig> baiduOssConfigMap = baiduOssProperties.getOssConfig();
        if (baiduOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, baiduOssClient(baiduOssProperties));
        } else {
            String endPoint = baiduOssProperties.getEndPoint();
            String accessKeyId = baiduOssProperties.getAccessKeyId();
            String secretAccessKey = baiduOssProperties.getSecretAccessKey();
            baiduOssConfigMap.forEach((name, baiduOssConfig) -> {
                if (ObjectUtil.isEmpty(baiduOssConfig.getEndPoint())) {
                    baiduOssConfig.setEndPoint(endPoint);
                }
                if (ObjectUtil.isEmpty(baiduOssConfig.getAccessKeyId())) {
                    baiduOssConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(baiduOssConfig.getSecretAccessKey())) {
                    baiduOssConfig.setSecretAccessKey(secretAccessKey);
                }
                SpringUtil.registerBean(name, baiduOssClient(baiduOssConfig));
            });
        }
        return null;
    }

    public StandardOssClient baiduOssClient(BaiduOssConfig baiduOssConfig) {
        return new BaiduOssClient(bosClient(bosClientConfiguration(baiduOssConfig)), baiduOssConfig);
    }

    public BosClientConfiguration bosClientConfiguration(BaiduOssConfig baiduOssConfig) {
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(baiduOssConfig.getAccessKeyId(), baiduOssConfig.getSecretAccessKey()));
        config.setEndpoint(baiduOssConfig.getEndPoint());
        return config;
    }

    public BosClient bosClient(BosClientConfiguration config) {
        return new BosClient(config);
    }

}
