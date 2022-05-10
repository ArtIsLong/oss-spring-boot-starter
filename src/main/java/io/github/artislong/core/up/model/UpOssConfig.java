package io.github.artislong.core.up.model;

import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version UpOssConfig.java, v 1.1 2022/2/20 9:13 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class UpOssConfig {

    private String basePath;
    private String bucketName;
    private String userName;
    private String password;

    private UpOssClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
