package io.github.artislong.core.pingan.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version PingAnOssConfig.java, v 1.1 2022/3/8 10:26 chenmin Exp $
 * Created on 2022/3/8
 */
@Data
public class PingAnOssConfig {

    private String userAgent;
    private String obsUrl;
    private String obsAccessKey;
    private String obsSecret;

    private String userId;

    private String basePath;
    private String bucketName;

    private Boolean representPathInKey = false;
    private String domainName;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
