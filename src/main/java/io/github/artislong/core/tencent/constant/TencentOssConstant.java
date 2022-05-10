package io.github.artislong.core.tencent.constant;

import com.qcloud.cos.utils.VersionInfoUtils;

/**
 * @author 陈敏
 * @version TencentOssConstant.java, v 1.0 2022/4/19 20:06 chenmin Exp $
 * Created on 2022/4/19
 */
public class TencentOssConstant {
    /**
     * 默认的获取连接的超时时间, 单位ms
      */
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = -1;
    /**
     * 默认连接超时, 单位ms
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;
    /**
     * 默认的SOCKET读取超时时间, 单位ms
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = 30 * 1000;
    /**
     * 默认的维护最大HTTP连接数
     */
    public static final int DEFAULT_MAX_CONNECTIONS_COUNT = 1024;
    /**
     * 空闲连接最长存活时间
     */
    public static final int DEFAULT_IDLE_CONNECTION_ALIVE = 60 * 1000;
    /**
     * 多次签名的默认过期时间,单位秒
     */
    public static final long DEFAULT_SIGN_EXPIRED = 3600;
    /**
     * 默认的user_agent标识
     */
    public static final String DEFAULT_USER_AGENT = VersionInfoUtils.getUserAgent();
    /**
     * Read Limit
     */
    public static final int DEFAULT_READ_LIMIT = (2 << 17) + 1;
    /**
     * 发生异常时的最大重试次数
     */
    public static final int DEFAULT_RETRY_TIMES = 3;

}
