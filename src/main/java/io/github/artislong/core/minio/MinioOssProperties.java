package io.github.artislong.core.minio;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.minio.model.MinioOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version MinioProperties.java, v 1.1 2021/11/24 15:20 chenmin Exp $
 * Created on 2021/11/24
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.MINIO)
public class MinioOssProperties extends MinioOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<MinioOssConfig> minioOssConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (minioOssConfigs.isEmpty()) {
            this.valid();
        } else {
            minioOssConfigs.forEach(MinioOssConfig::valid);
        }
    }
}
