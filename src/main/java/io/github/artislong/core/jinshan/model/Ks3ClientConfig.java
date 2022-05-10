package io.github.artislong.core.jinshan.model;

/**
 * @author 陈敏
 * @version Ks3ClientConfig.java, v 1.0 2022/5/2 21:30 chenmin Exp $
 * Created on 2022/5/2
 */
public class Ks3ClientConfig extends com.ksyun.ks3.service.Ks3ClientConfig {

    private SignerVersion version = SignerVersion.V2;

    @Override
    public SignerVersion getVersion() {
        return version;
    }

    @Override
    public void setVersion(SignerVersion version) {
        this.version = version;
    }
}
