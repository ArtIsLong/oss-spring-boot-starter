package io.github.artislong.core.huawei.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.obs.services.HttpProxyConfiguration;
import com.obs.services.ObsConfiguration;
import com.obs.services.internal.ext.ExtObsConfiguration;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.HttpProtocolTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.obs.services.internal.ObsConstraint.*;
import static com.obs.services.internal.ext.ExtObsConstraint.DEFAULT_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION;
import static com.obs.services.internal.ext.ExtObsConstraint.DEFAULT_RETRY_ON_CONNECTION_FAILURE_IN_OKHTTP;

/**
 * @author 陈敏
 * @version HuaweiOssClientConfig.java, v 1.0 2022/4/20 16:48 chenmin Exp $
 * Created on 2022/4/20
 */
@Data
@Accessors(chain = true)
public class HuaweiOssClientConfig {
    /**
     * 建立HTTP/HTTPS连接的超时时间（单位：毫秒）。默认为60000毫秒。
     */
    private int connectionTimeout = HTTP_CONNECT_TIMEOUT_VALUE;
    /**
     * 如果空闲时间超过此参数的设定值，则关闭连接（单位：毫秒）。默认为30000毫秒。
     */
    private int idleConnectionTime = DEFAULT_IDLE_CONNECTION_TIME;
    /**
     * 连接池中最大空闲连接数，默认值：1000。
     */
    private int maxIdleConnections = DEFAULT_MAX_IDLE_CONNECTIONS;
    /**
     * 最大允许的HTTP并发请求数。默认为1000。
     */
    private int maxConnections = HTTP_MAX_CONNECT_VALUE;
    /**
     * 请求失败（请求异常、服务端报500或503错误等）后最大的重试次数。默认3次。
     */
    private int maxErrorRetry = HTTP_RETRY_MAX_VALUE;
    /**
     * Socket层传输数据的超时时间（单位：毫秒）。默认为60000毫秒。
     */
    private int socketTimeout = HTTP_SOCKET_TIMEOUT_VALUE;
    /**
     * 设置HTTP请求的端口号 (默认为80)。
     */
    private int endpointHttpPort = HTTP_PORT_VALUE;
    /**
     * 设置HTTPS请求的端口号 (默认443)。
     */
    private int endpointHttpsPort = HTTPS_PORT_VALUE;
    /**
     * 指定是否使用HTTPS连接OBS (默认为 “true”)。
     */
    private Boolean httpsOnly = true;
    /**
     * 指定是否启用对OBS的路径样式访问。“true” 表示启用了路径样式的访问，而 “false” (默认) 表示启用了虚拟托管样式的访问。
     * 注意: 如果启用了路径样式访问，则不支持OBS 3.0的新bucket功能。
     */
    private Boolean pathStyle = false;
    /**
     * HTTP代理配置。默认为空。
     */
    private HttpProxyConfiguration httpProxy;
    /**
     * 上传流对象时使用的缓存大小（单位：字节）。默认为512KB。
     */
    private int uploadStreamRetryBufferSize;
    /**
     * 是否验证服务端证书。默认为false。
     */
    private Boolean validateCertificate = false;
    /**
     * 是否验证响应头信息的ContentType。默认为true。
     */
    private Boolean verifyResponseContentType = true;
    /**
     * 从Socket流下载对象的缓存大小（单位：字节），-1表示不设置缓存。默认为-1。
     */
    private int readBufferSize = -1;
    /**
     * 上传对象到Socket流时的缓存大小（单位：字节），-1表示不设置缓存。默认为-1。
     */
    private int writeBufferSize = -1;
    /**
     * 是否严格验证服务端主机名。默认为false。
     */
    private Boolean isStrictHostnameVerification = false;
    /**
     * 设置身份验证类型。
     */
    private AuthTypeEnum authType = AuthTypeEnum.OBS;
    /**
     * Socket发送缓冲区大小（单位：字节），对应java.net.SocketOptions.SO_SNDBUF参数。默认为-1表示不设置。
     */
    private int socketWriteBufferSize = -1;
    /**
     * Socket接收缓冲区大小（单位：字节），对应java.net.SocketOptions.SO_RCVBUF参数。默认为-1表示不设置。
     */
    private int socketReadBufferSize = -1;
    /**
     * 是否使用长连接访问OBS服务。默认为true。
     */
    private Boolean keepAlive = true;
    /**
     * 指定是否使用协议协商。
     */
    private Boolean authTypeNegotiation = true;
    /**
     * 是否通过自定义域名访问OBS服务。默认为false。
     */
    private Boolean cname = false;
    /**
     * 将文件夹隔离器设置为斜线。
     */
    private String delimiter = StrUtil.SLASH;
    /**
     * SSLContext的Provider，默认使用JDK提供的SSLContext。
     */
    private String sslProvider;
    /**
     * 访问OBS服务端时使用的HTTP协议类型。默认为HTTP1.1协议。
     */
    private HttpProtocolTypeEnum httpProtocolType = HttpProtocolTypeEnum.HTTP1_1;

    /**
     * 是否开启Okhttp中的连接失败重试，默认关闭
     */
    private Boolean retryOnConnectionFailureInOkhttp = DEFAULT_RETRY_ON_CONNECTION_FAILURE_IN_OKHTTP;
    /**
     * 发生异常时最大重试次数
     */
    private int maxRetryOnUnexpectedEndException = DEFAULT_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION;

    public ObsConfiguration toClientConfig() {
        ExtObsConfiguration obsConfiguration = new ExtObsConfiguration();
        BeanUtil.copyProperties(this, obsConfiguration,
                "readBufferSize", "writeBufferSize", "socketWriteBufferSize", "socketReadBufferSize");
        if (this.readBufferSize != -1) {
            obsConfiguration.setReadBufferSize(this.readBufferSize);
        }
        if (this.writeBufferSize != -1) {
            obsConfiguration.setWriteBufferSize(this.writeBufferSize);
        }
        if (this.socketWriteBufferSize != -1) {
            obsConfiguration.setSocketReadBufferSize(this.socketWriteBufferSize);
        }
        if (this.socketReadBufferSize != -1) {
            obsConfiguration.setSocketReadBufferSize(this.socketReadBufferSize);
        }
        return obsConfiguration;
    }
}
