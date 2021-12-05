package com.github.core.tencent;

import cn.hutool.core.text.CharPool;
import com.github.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version TencentProperties.java, v 1.1 2021/11/24 15:22 chenmin Exp $
 * Created on 2021/11/24
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.TENCENT)
public class TencentOssProperties {

    private String bucketName;
    private String secretId;
    private String secretKey;
    private String region;
}
