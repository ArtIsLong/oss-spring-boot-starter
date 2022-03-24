package io.github.artislong.core.qingyun.model;

import com.qingstor.sdk.config.EnvContext;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version QingYunOssConfig.java, v 1.0 2022/3/10 23:53 chenmin Exp $
 * Created on 2022/3/10
 */
@Data
public class QingYunOssConfig {

    private String endpoint;
    private String accessKey;
    private String accessSecret;

    private String bucketName;
    private String zone;
    private String basePath;

    private Boolean cnameSupport = false;
    private String additionalUserAgent;
    private Boolean virtualHostEnabled = false;

    private EnvContext.HttpConfig clientConfig = new EnvContext.HttpConfig();

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
