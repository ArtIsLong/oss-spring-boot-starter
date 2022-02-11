package io.github.artislong.core.baidu;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.model.SliceConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version BaiduProperties.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.BAIDU)
public class BaiduOssProperties implements InitializingBean {

    private String bucketName;
    private String endPoint;
    private String accessKeyId;
    private String secretAccessKey;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    @Override
    public void afterPropertiesSet() {
        this.getSliceConfig().valid();
    }
}
