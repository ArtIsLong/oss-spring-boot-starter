package io.github.artislong.core.up;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.upyun.ParallelUploader;
import com.upyun.RestManager;
import io.github.artislong.OssAutoConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.up.model.UpOssClientConfig;
import io.github.artislong.core.up.model.UpOssConfig;
import io.github.artislong.function.ThreeConsumer;
import io.github.artislong.model.SliceConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author 陈敏
 * @version UpOssConfiguration.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@SpringBootConfiguration
@ConditionalOnClass(RestManager.class)
@EnableConfigurationProperties({UpOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.UP + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class UpOssConfiguration extends OssAutoConfiguration {

    public static final String DEFAULT_BEAN_NAME = "upOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        UpOssProperties upOssProperties = getOssProperties(UpOssProperties.class, OssConstant.OssType.UP);
        Map<String, UpOssConfig> upOssConfigMap = upOssProperties.getOssConfig();
        if (upOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, UpOssClient.class, buildBeanProMap(upOssProperties));
        } else {
            String userName = upOssProperties.getUserName();
            String password = upOssProperties.getPassword();
            upOssConfigMap.forEach((name, upOssConfig) -> {
                if (ObjectUtil.isEmpty(upOssConfig.getUserName())) {
                    upOssConfig.setUserName(userName);
                }
                if (ObjectUtil.isEmpty(upOssConfig.getPassword())) {
                    upOssConfig.setPassword(password);
                }
                consumer.accept(name, UpOssClient.class, buildBeanProMap(upOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(UpOssConfig upOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        RestManager restManager = restManager(upOssConfig);
        ParallelUploader parallelUploader = parallelUploader(upOssConfig);
        beanProMap.put("restManager", restManager);
        beanProMap.put("parallelUploader", parallelUploader);
        beanProMap.put("upOssConfig", upOssConfig);
        return beanProMap;
    }

    public RestManager restManager(UpOssConfig upOssConfig) {
        RestManager restManager = new RestManager(upOssConfig.getBucketName(), upOssConfig.getUserName(), upOssConfig.getPassword());
        UpOssClientConfig clientConfig = Optional.ofNullable(upOssConfig.getClientConfig()).orElse(new UpOssClientConfig());
        // 手动设置超时时间：默认为30秒
        restManager.setTimeout(clientConfig.getTimeout());
        // 选择最优的接入点
        restManager.setApiDomain(clientConfig.getApiDomain().toString());
        return restManager;
    }

    public ParallelUploader parallelUploader(UpOssConfig upOssConfig) {
        ParallelUploader parallelUploader = new ParallelUploader(upOssConfig.getBucketName(), upOssConfig.getUserName(), upOssConfig.getPassword());

        SliceConfig sliceConfig = upOssConfig.getSliceConfig();
        parallelUploader.setParallel(sliceConfig.getTaskNum());
        parallelUploader.setCheckMD5(true);
        UpOssClientConfig clientConfig = Optional.ofNullable(upOssConfig.getClientConfig()).orElse(new UpOssClientConfig());
        parallelUploader.setTimeout(clientConfig.getTimeout());
        return parallelUploader;
    }
}
