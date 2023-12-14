package io.github.artislong.core.huawei;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import io.github.artislong.OssAutoConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.huawei.model.HuaweiOssClientConfig;
import io.github.artislong.core.huawei.model.HuaweiOssConfig;
import io.github.artislong.function.ThreeConsumer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author 陈敏
 * @version HuaWeiOssConfiguration.java, v 1.1 2021/11/25 9:58 chenmin Exp $
 * Created on 2021/11/25
 */
@SpringBootConfiguration
@ConditionalOnClass(ObsClient.class)
@EnableConfigurationProperties({HuaWeiOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.HUAWEI + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class HuaWeiOssConfiguration extends OssAutoConfiguration {

    public static final String DEFAULT_BEAN_NAME = "huaWeiOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        HuaWeiOssProperties huaWeiOssProperties = getOssProperties(HuaWeiOssProperties.class, OssConstant.OssType.HUAWEI);
        Map<String, HuaweiOssConfig> huaweiOssConfigMap = huaWeiOssProperties.getOssConfig();
        if (huaweiOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, HuaWeiOssClient.class, buildBeanProMap(huaWeiOssProperties));
        } else {
            String accessKey = huaWeiOssProperties.getAccessKey();
            String secretKey = huaWeiOssProperties.getSecretKey();
            String endPoint = huaWeiOssProperties.getEndPoint();
            HuaweiOssClientConfig clientConfig = huaWeiOssProperties.getClientConfig();
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
                if (ObjectUtil.isEmpty(huaweiOssConfig.getEndPoint())) {
                    huaweiOssConfig.setEndPoint(endPoint);
                }
                consumer.accept(name, HuaWeiOssClient.class, buildBeanProMap(huaweiOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(HuaweiOssConfig huaweiOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        ObsClient obsClient = obsClient(huaweiOssConfig);
        beanProMap.put("huaweiOssConfig", huaweiOssConfig);
        beanProMap.put("obsClient", obsClient);
        return beanProMap;
    }

    public ObsClient obsClient(HuaweiOssConfig huaweiOssConfig) {
        HuaweiOssClientConfig clientConfig = Optional.ofNullable(huaweiOssConfig.getClientConfig()).orElse(new HuaweiOssClientConfig());
        ObsConfiguration obsConfiguration = clientConfig.toClientConfig();
        obsConfiguration.setEndPoint(huaweiOssConfig.getEndPoint());
        return new ObsClient(huaweiOssConfig.getAccessKey(), huaweiOssConfig.getSecretKey(), obsConfiguration);
    }
}
