package io.github.artislong.core.tencent.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;

/**
 * @author 陈敏
 * @version TencentOssConfig.java, v 1.1 2022/2/20 9:10 chenmin Exp $
 * Created on 2022/2/20
 */
public class TencentOssConfig {

    private String basePath;
    private String bucketName;
    private String secretId;
    private String secretKey;

    private TencentOssClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public String getBasePath() {
        return basePath;
    }

    public TencentOssConfig setBasePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    public String getBucketName() {
        return bucketName;
    }

    public TencentOssConfig setBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public String getSecretId() {
        return secretId;
    }

    public TencentOssConfig setSecretId(String secretId) {
        this.secretId = secretId;
        return this;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public TencentOssConfig setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public TencentOssClientConfig getClientConfig() {
        return clientConfig;
    }

    public TencentOssConfig setClientConfig(TencentOssClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        return this;
    }

    public SliceConfig getSliceConfig() {
        return sliceConfig;
    }

    public TencentOssConfig setSliceConfig(SliceConfig sliceConfig) {
        this.sliceConfig = sliceConfig;
        return this;
    }

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
