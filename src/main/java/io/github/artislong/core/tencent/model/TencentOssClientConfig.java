package io.github.artislong.core.tencent.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import lombok.experimental.Accessors;

import static io.github.artislong.core.tencent.constant.TencentOssConstant.*;

/**
 * @author 陈敏
 * @version TencentOssClientConfig.java, v 1.0 2022/4/19 19:58 chenmin Exp $
 * Created on 2022/4/19
 */
@Data
@Accessors(chain = true)
public class TencentOssClientConfig {
    /**
     * 地域
     */
    private String region;
    /**
     * 连接OSS所采用的协议（HTTP或HTTPS），默认为HTTPS。
     */
    private HttpProtocol httpProtocol = HttpProtocol.https;
    /**
     * 域名后缀
     */
    private String endPointSuffix = null;
    /**
     * http proxy代理，如果使用http proxy代理，需要设置IP与端口
     */
    private String httpProxyIp = null;
    /**
     * 代理服务器端口
     */
    private int httpProxyPort = 0;
    /**
     * 代理服务器验证的用户名。
     */
    private String proxyUsername = null;
    /**
     * 代理服务器验证的密码。
     */
    private String proxyPassword = null;
    /**
     * 是否使用基本身份验证
     */
    private Boolean useBasicAuth = false;
    /**
     * 多次签名的过期时间,单位秒
     */
    private long signExpired = DEFAULT_SIGN_EXPIRED;
    /**
     * 获取连接的超时时间, 单位ms
     */
    private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
    /**
     * 默认连接超时, 单位ms
     */
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    /**
     * SOCKET读取超时时间, 单位ms
     */
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    /**
     * 最大HTTP连接数
     */
    private int maxConnectionsCount = DEFAULT_MAX_CONNECTIONS_COUNT;
    /**
     * 空闲连接存活时间
     */
    private int idleConnectionAlive = DEFAULT_IDLE_CONNECTION_ALIVE;
    /**
     * user_agent标识
     */
    private String userAgent = DEFAULT_USER_AGENT;
    /**
     * 读取限制
     */
    private int readLimit = DEFAULT_READ_LIMIT;
    /**
     * 数据万象特殊请求配置
     */
    private Boolean ciSpecialRequest = false;
    /**
     * 请求失败后最大的重试次数。默认3次。
     **/
    private int maxErrorRetry = DEFAULT_RETRY_TIMES;

    public ClientConfig toClientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        BeanUtil.copyProperties(this, clientConfig, new CopyOptions().setIgnoreNullValue(true).setIgnoreProperties("region"));
        if (ObjectUtil.isNotEmpty(region)) {
            clientConfig.setRegion(new Region(region));
        }
        return clientConfig;
    }
}
