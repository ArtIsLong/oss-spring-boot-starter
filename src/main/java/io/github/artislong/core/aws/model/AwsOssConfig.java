package io.github.artislong.core.aws.model;

import io.github.artislong.core.aws.constant.AwsRegion;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;

/**
 * @author 陈敏
 * @version AwsOssConfig.java, v 1.0 2022/4/1 18:05 chenmin Exp $
 * Created on 2022/4/1
 */
@Data
public class AwsOssConfig {
    /**
     * 数据存储路径
     */
    private String basePath;
    /**
     * Bucket名称
     */
    private String bucketName;

    private String accessKeyId;

    private String secretAccessKey;

    private AwsRegion region;

    private DefaultsMode mode;

    private AwsOssClientConfig clientConfig;
    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }
}
