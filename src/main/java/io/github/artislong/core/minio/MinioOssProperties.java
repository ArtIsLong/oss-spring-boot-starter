package io.github.artislong.core.minio;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version MinioProperties.java, v 1.1 2021/11/24 15:20 chenmin Exp $
 * Created on 2021/11/24
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.MINIO)
public class MinioOssProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;

}
