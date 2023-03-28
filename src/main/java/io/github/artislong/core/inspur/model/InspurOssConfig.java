package io.github.artislong.core.inspur.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 陈敏
 * @version InspurOssConfig.java, v 1.0 2022/5/17 13:11 chenmin Exp $
 * Created on 2022/5/17
 */
@Slf4j
@Data
public class InspurOssConfig {

    private String basePath;

    private String endpoint;
    private String accessKey;
    private String secretKey;

    private String bucketName;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }
}
