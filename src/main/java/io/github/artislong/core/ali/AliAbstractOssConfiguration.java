package io.github.artislong.core.ali;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import io.github.artislong.AbstractOssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ali.model.AliOssClientConfig;
import io.github.artislong.core.ali.model.AliOssConfig;
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
 * @version AliOssConfiguration.java, v 1.1 2021/11/16 15:25 chenmin Exp $
 * Created on 2021/11/16
 */
@SpringBootConfiguration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({AliOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.ALI + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class AliAbstractOssConfiguration extends AbstractOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "aliOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        AliOssProperties aliOssProperties = getOssProperties(AliOssProperties.class, OssConstant.OssType.ALI);
        Map<String, AliOssConfig> aliOssConfigMap = aliOssProperties.getOssConfig();
        if (aliOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, AliOssClient.class, buildBeanProMap(aliOssProperties));
        } else {
            String endpoint = aliOssProperties.getEndpoint();
            String accessKeyId = aliOssProperties.getAccessKeyId();
            String accessKeySecret = aliOssProperties.getAccessKeySecret();
            String securityToken = aliOssProperties.getSecurityToken();
            AliOssClientConfig clientConfig = aliOssProperties.getClientConfig();
            aliOssConfigMap.forEach((name, aliOssConfig) -> {
                if (ObjectUtil.isEmpty(aliOssConfig.getEndpoint())) {
                    aliOssConfig.setEndpoint(endpoint);
                }
                if (ObjectUtil.isEmpty(aliOssConfig.getAccessKeyId())) {
                    aliOssConfig.setAccessKeyId(accessKeyId);
                }
                if (ObjectUtil.isEmpty(aliOssConfig.getAccessKeySecret())) {
                    aliOssConfig.setAccessKeySecret(accessKeySecret);
                }
                if (ObjectUtil.isEmpty(aliOssConfig.getSecurityToken())) {
                    aliOssConfig.setSecurityToken(securityToken);
                }
                if (ObjectUtil.isEmpty(aliOssConfig.getClientConfig())) {
                    aliOssConfig.setClientConfig(clientConfig);
                }
                consumer.accept(name, AliOssClient.class, buildBeanProMap(aliOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(AliOssConfig aliOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        OSS oss = ossClient(aliOssConfig);
        beanProMap.put("aliOssConfig", aliOssConfig);
        beanProMap.put("oss", oss);
        return beanProMap;
    }

    public OSS ossClient(AliOssConfig aliOssConfig) {
        String securityToken = aliOssConfig.getSecurityToken();
        AliOssClientConfig clientConfiguration = aliOssConfig.getClientConfig();
        if (ObjectUtil.isEmpty(securityToken) && ObjectUtil.isNotEmpty(clientConfiguration)) {
            return new OSSClientBuilder().build(aliOssConfig.getEndpoint(),
                    aliOssConfig.getAccessKeyId(),
                    aliOssConfig.getAccessKeySecret(), clientConfiguration.toClientConfig());
        }
        if (ObjectUtil.isNotEmpty(securityToken) && ObjectUtil.isEmpty(clientConfiguration)) {
            return new OSSClientBuilder().build(aliOssConfig.getEndpoint(),
                    aliOssConfig.getAccessKeyId(),
                    aliOssConfig.getAccessKeySecret(), securityToken);
        }
        return new OSSClientBuilder().build(aliOssConfig.getEndpoint(),
                aliOssConfig.getAccessKeyId(),
                aliOssConfig.getAccessKeySecret(), securityToken,
                Optional.ofNullable(clientConfiguration).orElse(new AliOssClientConfig()).toClientConfig());
    }

}
