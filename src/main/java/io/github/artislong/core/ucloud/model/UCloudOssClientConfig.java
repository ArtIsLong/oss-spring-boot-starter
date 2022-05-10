package io.github.artislong.core.ucloud.model;

import cn.hutool.core.bean.BeanUtil;
import cn.ucloud.ufile.http.HttpClient;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.concurrent.TimeUnit;

import static cn.ucloud.ufile.http.HttpClient.Config.*;

/**
 * @author 陈敏
 * @version UCloudOssClientConfig.java, v 1.0 2022/4/21 10:01 chenmin Exp $
 * Created on 2022/4/21
 */
@Data
@Accessors(chain = true)
public class UCloudOssClientConfig {
    /**
     * 连接超时时间
     */
    private long timeoutConnect = DEFAULT_CONNECT_TIMEOUT;
    /**
     * 读超时时间
     */
    private long timeoutRead = DEFAULT_WRITE_TIMEOUT;
    /**
     * 写超时时间
     */
    private long timeoutWrite = DEFAULT_READ_TIMEOUT;
    /**
     * okhttp最大空闲连接数（5）
     */
    private int maxIdleConnections = DEFAULT_MAX_IDLE_CONNECTIONS;
    /**
     * okhttp活动链接存货时间（5分钟）
     */
    private long keepAliveDuration = DEFAULT_KEEP_ALIVE_DURATION_MINUTES;
    /**
     * okhttp活动链接存货时间单位, （分钟）
     */
    private TimeUnit keepAliveTimeUnit = DEFAULT_KEEP_ALIVE_DURATION_TIME_UNIT;

    public HttpClient.Config toClientConfig() {
        HttpClient.Config config = new HttpClient.Config();
        BeanUtil.copyProperties(this, config);
        return config;
    }
}
