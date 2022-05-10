package io.github.artislong.core.qingyun.model;

import com.qingstor.sdk.config.EnvContext;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author 陈敏
 * @version QingYunOssClientConfig.java, v 1.0 2022/4/20 18:13 chenmin Exp $
 * Created on 2022/4/20
 */
@Data
@Accessors(chain = true)
public class QingYunOssClientConfig {
    /**
     * 是否支持CNAME作为Endpoint，默认不支持CNAME。
     */
    private boolean cnameSupport = false;
    /**
     * 附加的用户代理
     */
    private String additionalUserAgent;
    /**
     * 是否启用虚拟Host
     */
    private boolean virtualHostEnabled = false;
    /**
     * 读超时时间
     */
    private int readTimeout = 100;
    /**
     * 写超时时间
     */
    private int writeTimeout = 100;
    /**
     * 连接超时时间
     */
    private int connectionTimeout = 60;

    public EnvContext.HttpConfig toClientConfig() {
        EnvContext.HttpConfig httpConfig = new EnvContext.HttpConfig();
        httpConfig.setConnectionTimeout(connectionTimeout);
        httpConfig.setReadTimeout(readTimeout);
        httpConfig.setWriteTimeout(writeTimeout);
        return httpConfig;
    }

}
