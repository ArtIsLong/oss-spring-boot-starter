package io.github.artislong.core.tencent.model;

import com.qcloud.cos.ClientConfig;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version TencentOssConfig.java, v 1.1 2022/2/20 9:10 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class TencentOssConfig {

    private String basePath;
    private String bucketName;
    private String secretId;
    private String secretKey;

    private ClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
