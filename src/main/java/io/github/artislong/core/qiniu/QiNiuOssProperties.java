package io.github.artislong.core.qiniu;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.qiniu.constant.QiNiuRegion;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version QiNiuOssProperties.java, v 1.1 2021/11/16 15:30 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.QINIU)
public class QiNiuOssProperties {
    private String accessKey;
    private String secretKey;
    private QiNiuRegion region = QiNiuRegion.AUTOREGION;
    private String bucketName;

}
