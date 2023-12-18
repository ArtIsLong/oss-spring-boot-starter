package io.github.artislong.core.ucloud;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.ucloud.ufile.UfileClient;
import cn.ucloud.ufile.api.object.ObjectApiBuilder;
import cn.ucloud.ufile.api.object.ObjectConfig;
import cn.ucloud.ufile.auth.ObjectAuthorization;
import cn.ucloud.ufile.auth.UfileObjectLocalAuthorization;
import io.github.artislong.OssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ucloud.model.UCloudOssClientConfig;
import io.github.artislong.core.ucloud.model.UCloudOssConfig;
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
 * @version UCloudOssConfiguration.java, v 1.1 2022/3/7 0:20 chenmin Exp $
 * Created on 2022/3/7
 */
@SpringBootConfiguration
@ConditionalOnClass(UfileClient.class)
@EnableConfigurationProperties({UCloudOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.UCLOUD + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class UCloudOssConfiguration extends OssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "uCloudOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        UCloudOssProperties uCloudOssProperties = getOssProperties(UCloudOssProperties.class, OssConstant.OssType.UCLOUD);
        Map<String, UCloudOssConfig> ossConfigMap = uCloudOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, UCloudOssClient.class, buildBeanProMap(uCloudOssProperties));
        } else {
            String publicKey = uCloudOssProperties.getPublicKey();
            String privateKey = uCloudOssProperties.getPrivateKey();
            String customHost = uCloudOssProperties.getCustomHost();
            UCloudOssClientConfig clientConfig = uCloudOssProperties.getClientConfig();
            ossConfigMap.forEach((name, ossConfig) -> {
                if (ObjectUtil.isEmpty(ossConfig.getPublicKey())) {
                    ossConfig.setPublicKey(publicKey);
                }
                if (ObjectUtil.isEmpty(ossConfig.getPrivateKey())) {
                    ossConfig.setPrivateKey(privateKey);
                }
                if (ObjectUtil.isEmpty(ossConfig.getCustomHost())) {
                    ossConfig.setCustomHost(customHost);
                }
                if (ObjectUtil.isEmpty(ossConfig.getClientConfig())) {
                    ossConfig.setClientConfig(clientConfig);
                }
                consumer.accept(name, UCloudOssClient.class, buildBeanProMap(ossConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(UCloudOssConfig uCloudOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        UCloudOssClientConfig clientConfig = Optional.ofNullable(uCloudOssConfig.getClientConfig()).orElse(new UCloudOssClientConfig());
        UfileClient.Config config = new UfileClient.Config(clientConfig.toClientConfig());
        ObjectAuthorization objectAuthorization = new UfileObjectLocalAuthorization(uCloudOssConfig.getPublicKey(), uCloudOssConfig.getPrivateKey());
        ObjectConfig objectConfig = new ObjectConfig(uCloudOssConfig.getCustomHost());
        UfileClient ufileClient = UfileClient.configure(config);
        ObjectApiBuilder objectApiBuilder = new ObjectApiBuilder(ufileClient, objectAuthorization, objectConfig);
        beanProMap.put("ufileClient", ufileClient);
        beanProMap.put("objectApiBuilder", objectApiBuilder);
        beanProMap.put("uCloudOssConfig", uCloudOssConfig);
        return beanProMap;
    }
}
