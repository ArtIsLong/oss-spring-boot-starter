package io.github.artislong.core.up;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.upyun.ParallelUploader;
import com.upyun.RestManager;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.up.model.UpOssConfig;
import io.github.artislong.model.SliceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author 陈敏
 * @version UpOssConfiguration.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@Configuration
@ConditionalOnClass(RestManager.class)
@EnableConfigurationProperties({UpOssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "up", havingValue = "true")
public class UpOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "upOssClient";

    @Autowired
    private UpOssProperties upOssProperties;

    @PostConstruct
    public void init() {
        List<UpOssConfig> upOssConfigs = upOssProperties.getUpOssConfigs();
        if (upOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, build(upOssProperties));
        } else {
            String userName = upOssProperties.getUserName();
            String password = upOssProperties.getPassword();
            for (int i = 0; i < upOssConfigs.size(); i++) {
                UpOssConfig upOssConfig = upOssConfigs.get(i);
                if (ObjectUtil.isEmpty(upOssConfig.getUserName())) {
                    upOssConfig.setUserName(userName);
                }
                if (ObjectUtil.isEmpty(upOssConfig.getPassword())) {
                    upOssConfig.setPassword(password);
                }
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), build(upOssConfig));
            }
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
