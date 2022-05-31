package io.github.artislong.core.wangyi.model;

import cn.hutool.core.bean.BeanUtil;
import com.netease.cloud.ClientConfiguration;
import com.netease.cloud.Protocol;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.netease.cloud.ClientConfiguration.*;

/**
 * @author 陈敏
 * @version WangYiOssClientConfig.java, v 1.0 2022/4/21 10:17 chenmin Exp $
 * Created on 2022/4/21
 */
@Data
@Accessors(chain = true)
public class WangYiOssClientConfig {
    /**
     * 连接超时时间
     */
    private int connectionTimeout = 50 * 1000;
    /**
     * 最大连接池大小。
     */
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    /**
     * 最大失败重试次数
     */
    private int maxErrorRetry = DEFAULT_MAX_RETRIES;
    /**
     * 是否使用子域名
     */
    private Boolean isSubDomain = true;
    /**
     * 连接OSS所采用的协议（HTTP或HTTPS），默认为HTTP。
     */
    private Protocol protocol = Protocol.HTTP;
    /**
     * 代理服务器的域，该域可以执行NTLM认证
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
     * 代理主机的NTLM身份验证服务器
     */
    private String proxyWorkstation;
    /**
     * Socket层传输数据的超时时间（单位：毫秒）。默认为50000毫秒。
     */
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    /**
     * 用户代理，指HTTP的User-Agent头
     */
    private String userAgent = DEFAULT_USER_AGENT;
    /**
     * Socket接收缓冲区的大小提示(以字节为单位)。
     */
    private int socketReceiveBufferSizeHint = 0;
    /**
     * Socket发送缓冲区的大小提示(以字节为单位)。
     */
    private int socketSendBufferSizeHint = 0;

    public ClientConfiguration toClientConfig() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        BeanUtil.copyProperties(this, clientConfiguration);
        return clientConfiguration;
    }
}
