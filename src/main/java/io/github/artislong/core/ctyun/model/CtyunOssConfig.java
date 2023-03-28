package io.github.artislong.core.ctyun.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 陈敏
 * @version CtyunOssConfig.java, v 1.0 2022/5/26 0:00 chenmin Exp $
 * Created on 2022/5/26
 */
@Slf4j
@Data
public class CtyunOssConfig {

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
