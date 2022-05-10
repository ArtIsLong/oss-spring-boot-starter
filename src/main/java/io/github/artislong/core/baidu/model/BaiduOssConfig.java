package io.github.artislong.core.baidu.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * @author 陈敏
 * @version BaiduOssConfig.java, v 1.1 2022/2/19 18:25 chenmin Exp $
 * Created on 2022/2/19
 */
@Slf4j
@Data
public class BaiduOssConfig {

    private String basePath;
    private String bucketName;
    private String accessKeyId;
    private String secretAccessKey;

    private BaiduOssClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
