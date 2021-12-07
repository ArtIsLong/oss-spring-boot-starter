package io.github.artislong.core.jd;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version JdOssProperties.java, v 1.1 2021/11/25 10:44 chenmin Exp $
 * Created on 2021/11/25
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.JD)
public class JdOssProperties {

    private String bucketName;
    private String endpoint;
    private String accessKey;
    private String secretKey;

    private String region;

}
