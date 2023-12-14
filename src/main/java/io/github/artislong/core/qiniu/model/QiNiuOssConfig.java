package io.github.artislong.core.qiniu.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version QiNiuOssConfig.java, v 1.1 2022/2/20 9:02 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class QiNiuOssConfig {

    private String basePath;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String domain;
    private long expireInSeconds = 3600;
    private QiNiuOssClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }
}
