package io.github.artislong.core.ali.model;

import cn.hutool.core.bean.BeanUtil;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.aliyun.oss.ClientConfiguration.*;

/**
 * @author 陈敏
 * @version AliOssClientConfig.java, v 1.0 2022/4/20 16:12 chenmin Exp $
 * Created on 2022/4/20
 */
@Data
@Accessors(chain = true)
public class AliOssClientConfig {
    /**
     * 用户代理，指HTTP的User-Agent头。默认为aliyun-sdk-java。
     */
    private String userAgent = DEFAULT_USER_AGENT;
    /**
     * 请求失败后最大的重试次数。默认3次。
     */
    private int maxErrorRetry = DEFAULT_MAX_RETRIES;
    /**
     * 从连接池中获取连接的超时时间（单位：毫秒）。默认不超时。
     */
    private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
    /**
     * 建立连接的超时时间（单位：毫秒）。默认为50000毫秒。
     */
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    /**
     * Socket层传输数据的超时时间（单位：毫秒）。默认为50000毫秒。
     */
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    /**
     * 允许打开的最大HTTP连接数。默认为1024
     */
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    /**
     * 连接TTL (生存时间)。Http连接由连接管理器用TTL缓存。
     */
    private long connectionTTL = DEFAULT_CONNECTION_TTL;
    /**
     * 是否使用com.aliyun.oss.common.comm.IdleConnectionReaper管理过期连接,默认开启
     */
    private Boolean useReaper = DEFAULT_USE_REAPER;
    /**
     * 连接空闲超时时间，超时则关闭连接（单位：毫秒）。默认为60000毫秒。
     */
    private long idleConnectionTime = DEFAULT_IDLE_CONNECTION_TIME;
    /**
     * 连接OSS所采用的协议（HTTP或HTTPS），默认为HTTP。
     */
    private Protocol protocol = Protocol.HTTP;
    /**
     * 代理服务器主机地址。
     */
    private String proxyHost = null;
    /**
     * 代理服务器端口。
     */
    private int proxyPort = -1;
    /**
     * 代理服务器验证的用户名。
     */
    private String proxyUsername = null;
    /**
     * 代理服务器验证的密码。
     */
    private String proxyPassword = null;
    /**
     * 代理服务器的域，该域可以执行NTLM认证
     */
    private String proxyDomain = null;
    /**
     * 代理主机的NTLM身份验证服务器
     */
    private String proxyWorkstation = null;
    /**
     * 是否支持CNAME作为Endpoint，默认支持CNAME。
     */
    private Boolean supportCname = false;
    /**
     * 设置不可变排除的CName列表 ---- 任何以该列表中的项目结尾的域都不会进行Cname解析。
     */
    private List<String> cnameExcludeList = new ArrayList<String>();
    /**
     * 是否开启二级域名（Second Level Domain）的访问方式，默认不开启。
     */
    private Boolean sldEnabled = false;
    /**
     * 请求超时时间，单位：毫秒。默认情况下是5分钟。
     */
    private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    /**
     * 是否启用请求超时校验。默认情况下，它是禁用的。
     */
    private Boolean requestTimeoutEnabled = false;
    /**
     * 设置慢请求的延迟阈值。如果请求的延迟大于延迟，则将记录该请求。默认情况下，阈值为5分钟。
     */
    private long slowRequestsThreshold = DEFAULT_SLOW_REQUESTS_THRESHOLD;
    /**
     * 设置默认的http头。所有请求头将自动添加到每个请求中。如果在请求中也指定了相同的请求头，则默认的标头将被覆盖。
     */
    private Map<String, String> defaultHeaders = new LinkedHashMap<String, String>();
    /**
     * 是否在上传和下载时启用CRC校验，默认启用
     */
    private Boolean crcCheckEnabled = true;
    /**
     * 所有请求设置签名版本
     */
    private SignVersion signatureVersion = DEFAULT_SIGNATURE_VERSION;
    /**
     * 设置OSS服务端时间和本地时间之间的差异，以毫秒为单位。
     */
    private long tickOffset = 0;
    /**
     * 是否开启HTTP重定向。
     * 说明: Java SDK 3.10.1及以上版本支持设置是否开启HTTP重定向，默认开启。
     */
    private Boolean redirectEnable = true;
    /**
     * 是否开启SSL证书校验。
     * 说明: Java SDK 3.10.1及以上版本支持设置是否开启SSL证书校验，默认开启。
     */
    private Boolean verifySSLEnable = true;
    /**
     * 是否开启日志记录连接池统计信息
     */
    private Boolean logConnectionPoolStats = false;
    /**
     * 是否使用系统属性值
     */
    private Boolean useSystemPropertyValues = false;

    public ClientBuilderConfiguration toClientConfig() {
        ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
        BeanUtil.copyProperties(this, clientConfig, "proxyPort", "tickOffset");
        if (this.proxyPort != -1) {
            clientConfig.setProxyPort(this.proxyPort);
        }
        if (this.tickOffset != 0) {
            clientConfig.setTickOffset(this.tickOffset);
        }
        return clientConfig;
    }
}
