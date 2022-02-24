package io.github.artislong.core.minio.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.PathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version MinioOssConfig.java, v 1.1 2022/2/20 8:58 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class MinioOssConfig {

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
        basePath = PathUtil.valid(basePath);
    }
}
