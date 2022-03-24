package io.github.artislong.core.huawei;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.huawei.model.HuaweiOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author 陈敏
 * @version HuaWeiOssConfiguration.java, v 1.1 2021/11/25 9:58 chenmin Exp $
 * Created on 2021/11/25
 */
@Configuration
@ConditionalOnClass(ObsClient.class)
@EnableConfigurationProperties({HuaWeiOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.HUAWEI + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class HuaWeiOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "huaWeiOssClient";

    @Autowired
    private HuaWeiOssProperties huaWeiOssProperties;

    @Bean
    public StandardOssClient huaWeiOssClient() {
        Map<String, HuaweiOssConfig> huaweiOssConfigMap = huaWeiOssProperties.getOssConfig();
        if (huaweiOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, huaWeiOssClient(huaWeiOssProperties));
        } else {
            String accessKey = huaWeiOssProperties.getAccessKey();
            String secretKey = huaWeiOssProperties.getSecretKey();
            ObsConfiguration clientConfig = huaWeiOssProperties.getClientConfig();
            huaweiOssConfigMap.forEach((name, huaweiOssConfig) -> {
                if (ObjectUtil.isEmpty(huaweiOssConfig.getAccessKey())) {
                    huaweiOssConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(huaweiOssConfig.getSecretKey())) {
                    huaweiOssConfig.setSecretKey(secretKey);
                }
                if (ObjectUtil.isEmpty(huaweiOssConfig.getClientConfig())) {
                    huaweiOssConfig.setClientConfig(clientConfig);
                }
                SpringUtil.registerBean(name, huaWeiOssClient(huaweiOssConfig));
            });
        }
        return null;
    }

    public StandardOssClient huaWeiOssClient(HuaweiOssConfig huaweiOssConfig) {
        return new HuaWeiOssClient(obsClient(huaweiOssConfig), huaweiOssConfig);
    }

    public ObsClient obsClient(HuaweiOssConfig huaweiOssConfig) {
        return new ObsClient(huaweiOssConfig.getAccessKey(), huaweiOssConfig.getSecretKey(), huaweiOssConfig.getClientConfig());
    }
}
