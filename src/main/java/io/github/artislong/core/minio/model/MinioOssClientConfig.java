package io.github.artislong.core.minio.model;

import io.github.artislong.constant.OssConstant;
import lombok.Data;

/**
 * @author 陈敏
 * @version MinioOssClientConfig.java, v 1.0 2022/3/24 9:59 chenmin Exp $
 * Created on 2022/3/24
 */
@Data
public class MinioOssClientConfig {
    private Long connectTimeout = OssConstant.DEFAULT_CONNECTION_TIMEOUT;
    private Long writeTimeout = OssConstant.DEFAULT_CONNECTION_TIMEOUT;
    private Long readTimeout = OssConstant.DEFAULT_CONNECTION_TIMEOUT;
}
