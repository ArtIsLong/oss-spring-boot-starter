package com.github.core.ali;

import cn.hutool.core.text.CharPool;
import com.github.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version AliOssProperties.java, v 1.1 2021/11/16 15:25 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.ALI)
public class AliOssProperties {
    /**
     * Bucket名称
     */
    private String bucketName;
    /**
     * OSS地址
     */
    private String endpoint;
    /**
     * AccessKey ID
     */
    private String accessKeyId;
    /**
     * AccessKey Secret
     */
    private String accessKeySecret;
}
