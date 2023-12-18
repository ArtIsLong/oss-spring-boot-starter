package io.github.artislong.core.local;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.OssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.local.model.LocalOssConfig;
import io.github.artislong.function.ThreeConsumer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Map;

/**
 * @author 陈敏
 * @version LocalConfiguration.java, v 1.1 2022/2/11 15:28 chenmin Exp $
 * Created on 2022/2/11
 */
@SpringBootConfiguration
@EnableConfigurationProperties({LocalOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.LOCAL + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE, matchIfMissing = true)
public class LocalOssConfiguration extends OssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "localOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        LocalOssProperties localProperties = getOssProperties(LocalOssProperties.class, OssConstant.OssType.LOCAL);
        Map<String, LocalOssConfig> localOssConfigMap = localProperties.getOssConfig();
        if (localOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, LocalOssClient.class, MapUtil.of("localOssConfig", localProperties));
        } else {
            localOssConfigMap.forEach((name, localOssConfig) -> consumer.accept(name, LocalOssClient.class, MapUtil.of("localOssConfig", localOssConfig)));
        }
    }
}
