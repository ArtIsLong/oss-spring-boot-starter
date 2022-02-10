package io.github.artislong.core.ali;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
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

    /**
     * 断点续传参数
     */
    private Slice slice;

    @Data
    static class Slice {
        /**
         * 分片大小,默认5MB
         */
        private Long partSize = 1024 * 1024 * 5L;

        /**
         * 上传并发线程数,默认等于CPU的核数
         */
        private Integer taskNum = Runtime.getRuntime().availableProcessors();
    }

}
