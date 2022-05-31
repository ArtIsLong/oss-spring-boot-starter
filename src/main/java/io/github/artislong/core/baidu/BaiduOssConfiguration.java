package io.github.artislong.core.baidu;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.baidu.model.BaiduOssClientConfig;
import io.github.artislong.core.baidu.model.BaiduOssConfig;
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
 * @version BaiduConfiguration.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@SpringBootConfiguration
@ConditionalOnClass(BosClient.class)
@EnableConfigurationProperties(BaiduOssProperties.class)
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.BAIDU + StrUtil.DOT + OssConstant.ENABLE,
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
            String accessKeyId = baiduOssProperties.getAccessKeyId();
            String secretAccessKey = baiduOssProperties.getSecretAccessKey();
            BaiduOssClientConfig clientConfig = baiduOssProperties.getClientConfig();
            baiduOssConfigMap.forEach((name, baiduOssConfig) -> {
                if (ObjectUtil.isEmpty(baiduOssConfig.getAccessKeyId())) {
                    baiduOssConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(baiduOssConfig.getSecretAccessKey())) {
                    baiduOssConfig.setSecretAccessKey(secretAccessKey);
                }
                if (ObjectUtil.isEmpty(baiduOssConfig.getClientConfig())) {
                    baiduOssConfig.setClientConfig(clientConfig);
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
        BaiduOssClientConfig clientConfig = Optional.ofNullable(baiduOssConfig.getClientConfig()).orElse(new BaiduOssClientConfig());
        BosClientConfiguration bosClientConfiguration = clientConfig.toClientConfig();
        bosClientConfiguration.setCredentials(new DefaultBceCredentials(baiduOssConfig.getAccessKeyId(), baiduOssConfig.getSecretAccessKey()));
        return bosClientConfiguration;
    }

    public BosClient bosClient(BosClientConfiguration config) {
        return new BosClient(config);
    }

}
