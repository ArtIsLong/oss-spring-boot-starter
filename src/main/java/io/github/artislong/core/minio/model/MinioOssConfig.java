package io.github.artislong.core.minio.model;

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

    public void init() {
        basePath = PathUtil.valid(basePath);
    }
}
