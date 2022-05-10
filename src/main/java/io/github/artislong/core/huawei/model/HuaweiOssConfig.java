package io.github.artislong.core.huawei.model;

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
    /**
     * 连接OBS的服务地址。可包含协议类型、域名、端口号。示例：https://your-endpoint:443。
     * （出于安全性考虑，建议使用https协议）
     */
    private String endPoint;
    private String bucketName;

    private HuaweiOssClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
