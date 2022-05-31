package io.github.artislong.core.baidu.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baidubce.BceClientConfiguration;
import com.baidubce.Protocol;
import com.baidubce.Region;
import com.baidubce.http.DefaultRetryPolicy;
import com.baidubce.http.RetryPolicy;
import com.baidubce.services.bos.BosClientConfiguration;
import io.github.artislong.core.baidu.constant.BaiduOssConstant;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.baidubce.BceClientConfiguration.*;
import static com.baidubce.services.bos.BosClientConfiguration.*;

/**
 * @author 陈敏
 * @version BaiduOssClientConfig.java, v 1.0 2022/4/20 16:19 chenmin Exp $
 * Created on 2022/4/20
 */
@Slf4j
@Data
@Accessors(chain = true)
public class BaiduOssClientConfig {
    /**
     * 使用cname访问BOS资源
     */
    private Boolean cnameEnabled;
    /**
     * 异步put
     */
    private Boolean enableHttpAsyncPut = true;
    /**
     * 建立连接的超时时间（单位：毫秒）
     */
    private int connectionTimeoutInMillis = DEFAULT_CONNECTION_TIMEOUT_IN_MILLIS;
    /**
     * 允许打开的最大HTTP连接数
     */
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    /**
     * 连接协议类型
     */
    private Protocol protocol = DEFAULT_PROTOCOL;
    /**
     * 访问NTLM验证的代理服务器的Windows域名
     */
    private String proxyDomain;
    /**
     * 代理服务器主机地址
     */
    private String proxyHost;
    /**
     * 代理服务器验证的密码
     */
    private String proxyPassword;
    /**
     * 代理服务器端口
     */
    private int proxyPort = -1;
    /**
     * 代理服务器验证的用户名
     */
    private String proxyUsername;
    /**
     * NTLM代理服务器的Windows工作站名称
     */
    private String proxyWorkstation;
    /**
     * 是否设置用户代理认证
     */
    private Boolean proxyPreemptiveAuthenticationEnabled;
    /**
     * 通过打开的连接传输数据的超时时间（单位：毫秒）
     */
    private int socketTimeoutInMillis = DEFAULT_SOCKET_TIMEOUT_IN_MILLIS;
    /**
     * Socket缓冲区大小
     */
    private int socketBufferSizeInBytes = 0;
    /**
     * 访问域名
     */
    private String endpoint = BaiduOssConstant.DEFAULT_ENDPOINT;
    /**
     * 地域
     */
    private Region region = DEFAULT_REGION;
    /**
     * 是否开启HTTP重定向。默认开启
     */
    private Boolean redirectsEnabled = true;
    /**
     * 本地地址
     */
    private String localAddress;
    /**
     * 请求失败最大重试次数
     */
    private int maxErrorRetry = RetryPolicy.DEFAULT_MAX_ERROR_RETRY;
    /**
     * 最大延迟时间，单位：毫秒
     */
    private long maxDelayInMillis = RetryPolicy.DEFAULT_MAX_DELAY_IN_MILLIS;
    /**
     * 流文件缓冲区大小
     */
    private int streamBufferSize = DEFAULT_STREAM_BUFFER_SIZE;
    /**
     * 用户代理，指HTTP的User-Agent头
     */
    private String userAgent = BceClientConfiguration.DEFAULT_USER_AGENT;

    public BosClientConfiguration toClientConfig() {
        BosClientConfiguration clientConfig = new BosClientConfiguration();
        BeanUtil.copyProperties(this, clientConfig, "proxyPort", "socketBufferSizeInBytes",
                "localAddress", "maxErrorRetry", "maxDelayInMillis");
        if (this.proxyPort != -1) {
            clientConfig.setProxyPort(this.proxyPort);
        }
        if (this.socketBufferSizeInBytes != 0) {
            clientConfig.setSocketBufferSizeInBytes(this.socketBufferSizeInBytes);
        }
        if (ObjectUtil.isNotEmpty(this.localAddress)) {
            try {
                clientConfig.setLocalAddress(InetAddress.getByName(this.localAddress));
            } catch (UnknownHostException e) {
                log.error("localAddress配置有误，请检查!", e);
            }
        }
        clientConfig.setRetryPolicy(new DefaultRetryPolicy(maxErrorRetry, maxDelayInMillis));
        return clientConfig;
    }
}
