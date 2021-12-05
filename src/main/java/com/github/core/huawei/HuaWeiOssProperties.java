package com.github.core.huawei;

import cn.hutool.core.text.CharPool;
import com.github.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version HuaWeiOssProperties.java, v 1.1 2021/11/25 9:56 chenmin Exp $
 * Created on 2021/11/25
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.HUAWEI)
public class HuaWeiOssProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

}
