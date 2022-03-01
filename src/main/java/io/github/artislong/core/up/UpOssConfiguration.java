package io.github.artislong.core.up;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.upyun.ParallelUploader;
import com.upyun.RestManager;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.up.model.UpOssConfig;
import io.github.artislong.model.SliceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author 陈敏
 * @version UpOssConfiguration.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@Configuration
@ConditionalOnClass(RestManager.class)
@EnableConfigurationProperties({UpOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.UP + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class UpOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "upOssClient";

    @Autowired
    private UpOssProperties upOssProperties;

    @Bean
    public void upOssClient() {
        Map<String, UpOssConfig> upOssConfigMap = upOssProperties.getOssConfig();
        if (upOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, build(upOssProperties));
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
                SpringUtil.registerBean(name, build(upOssConfig));
            });
        }
    }

    private StandardOssClient build(UpOssConfig upOssConfig) {
        RestManager restManager = restManager(upOssConfig);
        ParallelUploader parallelUploader = parallelUploader(upOssConfig);
        return upOssClient(restManager, parallelUploader, upOssConfig);
    }

    public StandardOssClient upOssClient(RestManager restManager, ParallelUploader parallelUploader, UpOssConfig upOssConfig) {
        return new UpOssClient(restManager, parallelUploader, upOssConfig);
    }

    public RestManager restManager(UpOssConfig upOssConfig) {
        RestManager restManager = new RestManager(upOssConfig.getBucketName(), upOssConfig.getUserName(), upOssConfig.getPassword());
        // 手动设置超时时间：默认为30秒
        restManager.setTimeout(upOssConfig.getTimeout());
        // 选择最优的接入点
        restManager.setApiDomain(upOssConfig.getApiDomain().toString());
        return restManager;
    }

    public ParallelUploader parallelUploader(UpOssConfig upOssConfig) {
        ParallelUploader parallelUploader = new ParallelUploader(upOssConfig.getBucketName(), upOssConfig.getUserName(), upOssConfig.getPassword());

        SliceConfig sliceConfig = upOssConfig.getSliceConfig();
        parallelUploader.setParallel(sliceConfig.getTaskNum());
        parallelUploader.setCheckMD5(true);

        return parallelUploader;
    }
}
