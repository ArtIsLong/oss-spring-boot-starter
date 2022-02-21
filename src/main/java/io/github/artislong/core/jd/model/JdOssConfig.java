package io.github.artislong.core.jd.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.PathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version JdOssConfig.java, v 1.1 2022/2/19 18:36 chenmin Exp $
 * Created on 2022/2/19
 */
@Data
public class JdOssConfig {

    private String basePath;

    private String bucketName;
    private String endpoint;
    private String accessKey;
    private String secretKey;

    private String region;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = PathUtil.valid(basePath);
    }

}
