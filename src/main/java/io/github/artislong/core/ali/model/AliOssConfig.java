package io.github.artislong.core.ali.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 陈敏
 * @version AliGroupInfo.java, v 1.1 2022/2/18 17:48 chenmin Exp $
 * Created on 2022/2/18
 */
@Data
@EqualsAndHashCode
public class AliOssConfig {
    /**
     * 数据存储路径
     */
    private String basePath;
    /**
     * Bucket名称
     */
    private String bucketName;
    /**
     * OSS地址
     */
    private String endpoint;
    /**
     * AccessKey ID
     */
    private String accessKeyId;
    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}