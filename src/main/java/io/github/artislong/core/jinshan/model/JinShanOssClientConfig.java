package io.github.artislong.core.jinshan.model;

import cn.hutool.core.bean.BeanUtil;
import com.ksyun.ks3.http.HttpClientConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 陈敏
 * @version JinShanOssClientConfig.java, v 1.0 2022/5/2 19:18 chenmin Exp $
 * Created on 2022/5/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JinShanOssClientConfig extends HttpClientConfig {
    /**
     * http或者https
     */
    private Ks3ClientConfig.PROTOCOL protocol = Ks3ClientConfig.PROTOCOL.http;
    /**
     * 签名版本
     */
    private Ks3ClientConfig.SignerVersion version = Ks3ClientConfig.SignerVersion.V2;
    /**
     * 是否使用path style access方式访问
     */
    private Boolean pathStyleAccess = false;
    /**
     * 允许客户端发送匿名请求
     */
    private Boolean allowAnonymous = true;
    /**
     * 当服务端返回307时是否自动跳转，
     * 主要发生在用Region A的endpoint请求Region B的endpoint
     */
    private Boolean flowRedirect = true;
    /**
     * 是否使用绑定的域名作为endpoint
     */
    private Boolean domainMode = false;
    /**
     * 签名类
     */
    private String signerClass = "com.ksyun.ks3.signer.DefaultSigner";
    private Boolean useGzip = false;

    public Ks3ClientConfig toClientConfig() {
        Ks3ClientConfig clientConfig = new Ks3ClientConfig();
        BeanUtil.copyProperties(this, clientConfig);
        return clientConfig;
    }

    public HttpClientConfig toHttpClientConfig() {
        HttpClientConfig httpClientConfig = new HttpClientConfig();
        BeanUtil.copyProperties(this, httpClientConfig);
        return httpClientConfig;
    }

}
