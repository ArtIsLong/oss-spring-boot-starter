package io.github.artislong.core.huawei;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.model.SliceConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version HuaWeiOssProperties.java, v 1.1 2021/11/25 9:56 chenmin Exp $
 * Created on 2021/11/25
 */
@Slf4j
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.HUAWEI)
public class HuaWeiOssProperties implements InitializingBean {

    private String endpoint;
    private String accessKey;
    private String secretKey;
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
