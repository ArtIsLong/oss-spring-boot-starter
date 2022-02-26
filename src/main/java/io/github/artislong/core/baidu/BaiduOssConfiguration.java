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

import javax.annotation.PostConstruct;
import java.util.List;

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
    public void init() {
        List<BaiduOssConfig> baiduOssConfigs = baiduOssProperties.getOssConfigs();
        if (baiduOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, baiduOssClient(bosClient(bosClientConfiguration(baiduOssProperties)), baiduOssProperties));
        } else {
            String endPoint = baiduOssProperties.getEndPoint();
            String accessKeyId = baiduOssProperties.getAccessKeyId();
            String secretAccessKey = baiduOssProperties.getSecretAccessKey();
            for (int i = 0; i < baiduOssConfigs.size(); i++) {
                BaiduOssConfig baiduOssConfig = baiduOssConfigs.get(i);
                if (ObjectUtil.isEmpty(baiduOssConfig.getEndPoint())) {
                    baiduOssConfig.setEndPoint(endPoint);
                }
                if (ObjectUtil.isEmpty(baiduOssConfig.getAccessKeyId())) {
                    baiduOssConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(baiduOssConfig.getSecretAccessKey())) {
                    baiduOssConfig.setSecretAccessKey(secretAccessKey);
                }
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), baiduOssClient(bosClient(bosClientConfiguration(baiduOssConfig)), baiduOssConfig));
            }
        }
    }

    public StandardOssClient baiduOssClient(BosClient bosClient, BaiduOssConfig baiduOssConfig) {
        return new BaiduOssClient(bosClient, baiduOssConfig);
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
