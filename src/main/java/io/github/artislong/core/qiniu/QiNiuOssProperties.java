package io.github.artislong.core.qiniu;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.qiniu.constant.QiNiuRegion;
import io.github.artislong.model.SliceConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version QiNiuOssProperties.java, v 1.1 2021/11/16 15:30 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.QINIU)
public class QiNiuOssProperties implements InitializingBean {
    private String accessKey;
    private String secretKey;
    private QiNiuRegion region = QiNiuRegion.AUTOREGION;
    private String bucketName;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    @Override
    public void afterPropertiesSet() {
        this.getSliceConfig().valid();
    }

}
