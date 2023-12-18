package io.github.artislong.core.qiniu.model;

import cn.hutool.core.util.ObjectUtil;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version QiNiuOssConfig.java, v 1.1 2022/2/20 9:02 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class QiNiuOssConfig {

    private String basePath;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    /**
     * bucket域名
     */
    private String domain;
    /**
     * 私有空间外网链接超时时长
     * 单位：秒
     */
    private long expireInPrivate = 3600;
    private QiNiuOssClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

    public void validate() {
        if (ObjectUtil.isEmpty(accessKey) ||
                ObjectUtil.isEmpty(secretKey) ||
                ObjectUtil.isEmpty(bucketName) ||
                ObjectUtil.isEmpty(domain)) {
            throw new OssException("bucketName或domain为空，请检查配置！");
        }
    }
}
