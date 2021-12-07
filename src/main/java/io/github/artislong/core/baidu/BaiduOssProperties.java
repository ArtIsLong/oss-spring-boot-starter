package io.github.artislong.core.baidu;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version BaiduProperties.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.BAIDU)
public class BaiduOssProperties {

    private String bucketName;
    private String endPoint;
    private String accessKeyId;
    private String secretAccessKey;
}
