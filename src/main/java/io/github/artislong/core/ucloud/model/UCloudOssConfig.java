package io.github.artislong.core.ucloud.model;

import cn.ucloud.ufile.http.HttpClient;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version UCloudOssConfig.java, v 1.1 2022/3/7 0:20 chenmin Exp $
 * Created on 2022/3/7
 */
@Data
public class UCloudOssConfig {

    private String basePath;
    private String bucketName;
    private String publicKey;
    private String privateKey;
    private String region;
    private String proxySuffix;

    private HttpClient.Config clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
