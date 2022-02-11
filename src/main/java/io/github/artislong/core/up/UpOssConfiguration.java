package io.github.artislong.core.up;

import com.upyun.ParallelUploader;
import com.upyun.RestManager;
import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.model.SliceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version UpOssConfiguration.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@Configuration
@ConditionalOnClass(RestManager.class)
@EnableConfigurationProperties({UpOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.UP)
public class UpOssConfiguration {

    @Autowired
    private UpOssProperties upOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient upOssClient(RestManager restManager, ParallelUploader parallelUploader) {
        return new UpOssClient(restManager, parallelUploader, ossProperties, upOssProperties);
    }

    @Bean
    public RestManager restManager() {
        RestManager restManager = new RestManager(upOssProperties.getBucketName(), upOssProperties.getUserName(), upOssProperties.getPassword());
        // 手动设置超时时间：默认为30秒
        restManager.setTimeout(upOssProperties.getTimeout());
        // 选择最优的接入点
        restManager.setApiDomain(upOssProperties.getApiDomain());
        return restManager;
    }

    @Bean
    public ParallelUploader parallelUploader() {
        ParallelUploader parallelUploader = new ParallelUploader(upOssProperties.getBucketName(), upOssProperties.getUserName(), upOssProperties.getPassword());

        SliceConfig sliceConfig = upOssProperties.getSliceConfig();
        parallelUploader.setParallel(sliceConfig.getTaskNum());
        parallelUploader.setCheckMD5(true);

        return parallelUploader;
    }
}
