package io.github.artislong.core.baidu;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import io.github.artislong.AbstractOssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.baidu.model.BaiduOssClientConfig;
import io.github.artislong.core.baidu.model.BaiduOssConfig;
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
 * @version BaiduConfiguration.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@SpringBootConfiguration
@ConditionalOnClass(BosClient.class)
@EnableConfigurationProperties(BaiduOssProperties.class)
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.BAIDU + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class BaiduAbstractOssConfiguration extends AbstractOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "baiduOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        BaiduOssProperties baiduOssProperties = getOssProperties(BaiduOssProperties.class, OssConstant.OssType.BAIDU);
        Map<String, BaiduOssConfig> baiduOssConfigMap = baiduOssProperties.getOssConfig();
        if (baiduOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, BaiduOssClient.class, buildBeanProMap(baiduOssProperties));
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
                consumer.accept(name, BaiduOssClient.class, buildBeanProMap(baiduOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(BaiduOssConfig baiduOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        BosClient bosClient = bosClient(bosClientConfiguration(baiduOssConfig));
        beanProMap.put("bosClient", bosClient);
        beanProMap.put("baiduOssConfig", baiduOssConfig);
        return beanProMap;
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
