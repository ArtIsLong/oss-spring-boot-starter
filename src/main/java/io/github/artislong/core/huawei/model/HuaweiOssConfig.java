package io.github.artislong.core.huawei.model;

import com.obs.services.ObsConfiguration;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 陈敏
 * @version HuaweiOssConfig.java, v 1.1 2022/2/19 18:33 chenmin Exp $
 * Created on 2022/2/19
 */
@Slf4j
@Data
public class HuaweiOssConfig {

    private String basePath;

    private String accessKey;
    private String secretKey;
    private String bucketName;

    private ObsConfiguration clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
