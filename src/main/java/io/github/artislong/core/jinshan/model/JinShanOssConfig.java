package io.github.artislong.core.jinshan.model;

import com.ksyun.ks3.service.Ks3ClientConfig;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version JinShanOssConfig.java, v 1.1 2022/3/3 22:28 chenmin Exp $
 * Created on 2022/3/3
 */
@Data
public class JinShanOssConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String basePath;

    private Ks3ClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
