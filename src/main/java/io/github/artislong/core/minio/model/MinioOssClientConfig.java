package io.github.artislong.core.minio.model;

import io.github.artislong.constant.OssConstant;
import lombok.Data;

/**
 * @author 陈敏
 * @version MinioOssClientConfig.java, v 1.0 2022/3/24 9:59 chenmin Exp $
 * Created on 2022/3/24
 */
@Data
public class MinioOssClientConfig {
    /**
     * 连接超时时间（单位：毫秒）
     */
    private Long connectTimeout = OssConstant.DEFAULT_CONNECTION_TIMEOUT;
    /**
     * 写超时时间（单位：毫秒）
     */
    private Long writeTimeout = OssConstant.DEFAULT_CONNECTION_TIMEOUT;
    /**
     * 读超时时间（单位：毫秒）
     */
    private Long readTimeout = OssConstant.DEFAULT_CONNECTION_TIMEOUT;
    /**
     * 调用超时时间（单位：毫秒）
     */
    private Long callTimeout = (long) Integer.MAX_VALUE;
    /**
     * 是否支持重定向，默认支持
     */
    private boolean followRedirects = true;
    /**
     * 是否支持HTTP到HTTPS，HTTPS到HTTP的重定向，默认支持
     */
    private boolean followSslRedirects = true;
    /**
     * 是否开始连接失败重试，默认不支持
     */
    private boolean retryOnConnectionFailure = false;
    /**
     * 连接健康检测间隔时长（单位：毫秒）
     */
    private int pingInterval;
}
