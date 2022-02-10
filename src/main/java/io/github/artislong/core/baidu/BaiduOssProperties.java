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

    /**
     * 断点续传参数
     */
    private Slice slice;

    @Data
    static class Slice {
        /**
         * 分块大小，默认为5MB
         */
        private Long partSize = 1024 * 1024 * 5L;

        /**
         * 分块上传中线程池中线程的数量，默认等于CPU的核数
         */
        private Integer taskNum = Runtime.getRuntime().availableProcessors();
    }
}
