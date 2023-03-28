package io.github.artislong.core.ecloud.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 陈敏
 * @version EcloudOssConfig.java, v 1.0 2022/5/26 0:01 chenmin Exp $
 * Created on 2022/5/26
 */
@Slf4j
@Data
public class EcloudOssConfig {

    private String basePath;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }
}
