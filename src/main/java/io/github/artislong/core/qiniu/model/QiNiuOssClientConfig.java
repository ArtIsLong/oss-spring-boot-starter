package io.github.artislong.core.qiniu.model;

import com.qiniu.common.Constants;
import com.qiniu.storage.Configuration;
import io.github.artislong.core.qiniu.constant.QiNiuRegion;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author 陈敏
 * @version QiNiuOssClientConfig.java, v 1.0 2022/4/20 18:23 chenmin Exp $
 * Created on 2022/4/20
 */
@Data
@Accessors(chain = true)
public class QiNiuOssClientConfig {

    /**
     * 使用的Region
     */
    public QiNiuRegion region = QiNiuRegion.AUTOREGION;
    /**
     * 空间相关上传管理操作是否使用 https , 默认 是
     */
    public Boolean useHttpsDomains = true;
    /**
     * 空间相关上传管理操作是否使用代理加速上传，默认 是
     */
    public Boolean accUpHostFirst = true;
    /**
     * 使用 AutoRegion 时，如果从区域信息得到上传 host 失败，使用默认的上传域名上传，默认 是
     * upload.qiniup.com, upload-z1.qiniup.com, upload-z2.qiniup.com,
     * upload-na0.qiniup.com, upload-as0.qiniup.com
     */
    public Boolean useDefaultUpHostIfNone = true;
    /**
     * 如果文件大小大于此值则使用断点上传, 否则使用Form上传
     */
    public int putThreshold = Constants.BLOCK_SIZE;
    /**
     * 连接超时时间 单位秒(默认10s)
     */
    public int connectTimeout = Constants.CONNECT_TIMEOUT;
    /**
     * 写超时时间 单位秒(默认 0 , 不超时)
     */
    public int writeTimeout = Constants.WRITE_TIMEOUT;
    /**
     * 回复超时时间 单位秒(默认30s)
     */
    public int readTimeout = Constants.READ_TIMEOUT;
    /**
     * 底层HTTP库所有的并发执行的请求数量
     */
    public int dispatcherMaxRequests = Constants.DISPATCHER_MAX_REQUESTS;
    /**
     * 底层HTTP库对每个独立的Host进行并发请求的数量
     */
    public int dispatcherMaxRequestsPerHost = Constants.DISPATCHER_MAX_REQUESTS_PER_HOST;
    /**
     * 底层HTTP库中复用连接对象的最大空闲数量
     */
    public int connectionPoolMaxIdleCount = Constants.CONNECTION_POOL_MAX_IDLE_COUNT;
    /**
     * 底层HTTP库中复用连接对象的回收周期（单位分钟）
     */
    public int connectionPoolMaxIdleMinutes = Constants.CONNECTION_POOL_MAX_IDLE_MINUTES;
    /**
     * 上传失败重试次数
     */
    public int retryMax = 5;

    public Configuration toClientConfig() {
        Configuration configuration = new Configuration();
        configuration.region = this.region.buildRegion();
        configuration.useHttpsDomains = useHttpsDomains;
        configuration.accUpHostFirst = accUpHostFirst;
        configuration.useDefaultUpHostIfNone = useDefaultUpHostIfNone;
        configuration.putThreshold = putThreshold;
        configuration.connectTimeout = connectTimeout;
        configuration.writeTimeout = writeTimeout;
        configuration.readTimeout = readTimeout;
        configuration.dispatcherMaxRequests = dispatcherMaxRequests;
        configuration.dispatcherMaxRequestsPerHost = dispatcherMaxRequestsPerHost;
        configuration.connectionPoolMaxIdleCount = connectionPoolMaxIdleCount;
        configuration.connectionPoolMaxIdleMinutes = connectionPoolMaxIdleMinutes;
        configuration.retryMax = retryMax;
        return configuration;
    }

}
