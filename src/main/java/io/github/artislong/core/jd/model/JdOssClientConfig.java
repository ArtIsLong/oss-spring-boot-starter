package io.github.artislong.core.jd.model;

import cn.hutool.core.bean.BeanUtil;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.retry.RetryMode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.ClientConfiguration.*;

/**
 * @author 陈敏
 * @version JdOssClientConfig.java, v 1.0 2022/4/20 17:19 chenmin Exp $
 * Created on 2022/4/20
 */
@Data
@Accessors(chain = true)
public class JdOssClientConfig {
    /**
     * 建立连接的超时时间（单位：毫秒）。默认为50000毫秒
     */
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    /**
     * 允许打开的最大HTTP连接数。默认为1024
     */
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    /**
     * 请求失败后最大的重试次数
     */
    private int maxErrorRetry = -1;
    /**
     * 是否限制重试的默认值。
     */
    private boolean throttleRetries = DEFAULT_THROTTLE_RETRIES;
    /**
     * 连接协议类型
     */
    private Protocol protocol = Protocol.HTTPS;
    /**
     * 连接到HTTP代理时要使用的协议。
     */
    private Protocol proxyProtocol = Protocol.HTTP;
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
     * 指定不通过代理应访问的主机。
     */
    private String nonProxyHosts;
    /**
     * 是否禁用Socket代理
     */
    private boolean disableSocketProxy = DEFAULT_DISABLE_SOCKET_PROXY;
    /**
     * 是否使用基本身份验证对代理服务器进行抢先身份验证
     */
    private boolean preemptiveBasicProxyAuth;
    /**
     * Socket层传输数据的超时时间（单位：毫秒）。默认为50000毫秒
     */
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    /**
     * 请求的默认超时时间。默认为0，禁用的。
     */
    private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    /**
     * 请求的默认超时时间。默认为0，禁用的。
     */
    private int clientExecutionTimeout = DEFAULT_CLIENT_EXECUTION_TIMEOUT;
    /**
     * 公共HTTP请求头前缀。
     */
    private String userAgentPrefix = DEFAULT_USER_AGENT;
    /**
     * 公共HTTP请求头后缀。
     */
    private String userAgentSuffix;
    /**
     * 是否使用com.amazonaws.http.IdleConnectionReaper管理旧连接
     */
    private boolean useReaper = DEFAULT_USE_REAPER;
    /**
     * 是否使用gzip解压缩
     */
    private boolean useGzip = DEFAULT_USE_GZIP;
    /**
     * Socket发送缓冲区的大小提示(以字节为单位)。
     */
    private int socketSendBufferSizeHint = 0;
    /**
     * Socket接收缓冲区的大小提示(以字节为单位)。
     */
    private int socketReceiveBufferSizeHint = 0;
    /**
     * 设置签名算法对请求进行签名。如果未明确设置，客户端将通过提取SDK中的配置文件来确定要使用的算法
     */
    private String signerOverride;
    /**
     * 响应元数据缓存大小
     */
    private int responseMetadataCacheSize = DEFAULT_RESPONSE_METADATA_CACHE_SIZE;
    /**
     * 是否使用USE_EXPECT_CONTINUE作为期望值
     */
    private boolean useExpectContinue = DEFAULT_USE_EXPECT_CONTINUE;
    /**
     * 是否缓存响应元数据
     */
    private boolean cacheResponseMetadata = DEFAULT_CACHE_RESPONSE_METADATA;
    /**
     * 连接TTL (生存时间)。Http连接由连接管理器用TTL缓存。
     */
    private long connectionTTL = DEFAULT_CONNECTION_TTL;
    /**
     * 连接池中连接的最大空闲时间 (以毫秒为单位)。
     */
    private long connectionMaxIdleMillis = DEFAULT_CONNECTION_MAX_IDLE_MILLIS;
    /**
     * 在必须验证连接是否仍处于打开状态之前，连接可以在连接池中处于空闲状态。
     */
    private int validateAfterInactivityMillis = DEFAULT_VALIDATE_AFTER_INACTIVITY_MILLIS;
    /**
     * 是否使用TCP KeepAlive的默认值。
     */
    private boolean tcpKeepAlive = DEFAULT_TCP_KEEP_ALIVE;
    /**
     * 所有请求的公共请求头
     */
    private Map<String, String> headers = new HashMap<>();
    /**
     * 请求失败最大重试次数
     */
    private int maxConsecutiveRetriesBeforeThrottling = DEFAULT_MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING;
    /**
     * 是否禁用主机前缀
     */
    private boolean disableHostPrefixInjection;
    /**
     * 重试模式
     */
    private RetryMode retryMode;

    public ClientConfiguration toClientConfig() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        BeanUtil.copyProperties(this, clientConfiguration,
                "maxErrorRetry", "proxyPort");
        if (this.maxErrorRetry != -1) {
            clientConfiguration.setMaxErrorRetry(this.maxErrorRetry);
        }
        if (this.proxyPort != -1) {
            clientConfiguration.setProxyPort(this.proxyPort);
        }
        return clientConfiguration;
    }

}
