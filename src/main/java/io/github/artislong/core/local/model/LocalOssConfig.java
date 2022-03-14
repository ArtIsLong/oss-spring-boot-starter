package io.github.artislong.core.local.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version LocalOssConfig.java, v 1.1 2022/2/20 8:55 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class LocalOssConfig {

    /**
     * 数据存储路径
     */
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
