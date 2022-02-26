package io.github.artislong.core.huawei;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.obs.services.ObsClient;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.huawei.model.HuaweiOssConfig;
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
    public void init() {
        List<HuaweiOssConfig> huaweiOssConfigs = huaWeiOssProperties.getOssConfigs();
        if (huaweiOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, huaWeiOssClient(obsClient(huaWeiOssProperties), huaWeiOssProperties));
        } else {
            String endpoint = huaWeiOssProperties.getEndpoint();
            String accessKey = huaWeiOssProperties.getAccessKey();
            String secretKey = huaWeiOssProperties.getSecretKey();
            for (int i = 0; i < huaweiOssConfigs.size(); i++) {
                HuaweiOssConfig huaweiOssConfig = huaweiOssConfigs.get(i);
                if (ObjectUtil.isEmpty(huaweiOssConfig.getEndpoint())) {
                    huaweiOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(huaweiOssConfig.getAccessKey())) {
                    huaweiOssConfig.setAccessKey(accessKey);
                }
                if (ObjectUtil.isEmpty(huaweiOssConfig.getSecretKey())) {
                    huaweiOssConfig.setSecretKey(secretKey);
                }
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), huaWeiOssClient(obsClient(huaweiOssConfig), huaweiOssConfig));
            }
        }
    }

    public StandardOssClient huaWeiOssClient(ObsClient obsClient, HuaweiOssConfig huaweiOssConfig) {
        return new HuaWeiOssClient(obsClient, huaweiOssConfig);
    }

    public ObsClient obsClient(HuaweiOssConfig huaweiOssConfig) {
        return new ObsClient(huaweiOssConfig.getAccessKey(), huaweiOssConfig.getSecretKey(), huaweiOssConfig.getEndpoint());
    }
}
