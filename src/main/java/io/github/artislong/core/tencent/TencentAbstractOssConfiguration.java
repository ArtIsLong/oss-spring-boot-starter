package io.github.artislong.core.tencent;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import io.github.artislong.AbstractOssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.tencent.model.TencentOssClientConfig;
import io.github.artislong.core.tencent.model.TencentOssConfig;
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
 * @version TencentConfiguration.java, v 1.1 2021/11/24 15:22 chenmin Exp $
 * Created on 2021/11/24
 */
@SpringBootConfiguration
@ConditionalOnClass(COSClient.class)
@EnableConfigurationProperties({TencentOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.TENCENT + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class TencentAbstractOssConfiguration extends AbstractOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "tencentOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        TencentOssProperties tencentOssProperties = getOssProperties(TencentOssProperties.class, OssConstant.OssType.TENCENT);
        Map<String, TencentOssConfig> tencentOssConfigMap = tencentOssProperties.getOssConfig();
        if (tencentOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, TencentOssClient.class, buildBeanProMap(tencentOssProperties));
        } else {
            String secretId = tencentOssProperties.getSecretId();
            String secretKey = tencentOssProperties.getSecretKey();
            TencentOssClientConfig clientConfig = tencentOssProperties.getClientConfig();
            tencentOssConfigMap.forEach((name, tencentOssConfig) -> {
                if (ObjectUtil.isEmpty(tencentOssConfig.getSecretId())) {
                    tencentOssConfig.setSecretId(secretId);
                }
                if (ObjectUtil.isEmpty(tencentOssConfig.getSecretKey())) {
                    tencentOssConfig.setSecretKey(secretKey);
                }
                if (ObjectUtil.isEmpty(tencentOssConfig.getClientConfig())) {
                    tencentOssConfig.setClientConfig(clientConfig);
                }
                consumer.accept(name, TencentOssClient.class, buildBeanProMap(tencentOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(TencentOssConfig tencentOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        TencentOssClientConfig clientConfig = Optional.ofNullable(tencentOssConfig.getClientConfig()).orElse(new TencentOssClientConfig());
        COSCredentials cosCredentials = cosCredentials(tencentOssConfig);
        COSClient cosClient = cosClient(cosCredentials, clientConfig.toClientConfig());
        beanProMap.put("cosClient", cosClient);
        beanProMap.put("tencentOssConfig", tencentOssConfig);
        return beanProMap;
    }

    public COSCredentials cosCredentials(TencentOssConfig tencentOssConfig) {
        return new BasicCOSCredentials(tencentOssConfig.getSecretId(), tencentOssConfig.getSecretKey());
    }

    public COSClient cosClient(COSCredentials cred, ClientConfig clientConfig) {
        return new COSClient(cred, clientConfig);
    }
}
