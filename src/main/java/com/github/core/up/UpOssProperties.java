package com.github.core.up;

import cn.hutool.core.text.CharPool;
import com.github.constant.OssConstant;
import com.upyun.RestManager;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version UpOssProperties.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.UP)
public class UpOssProperties {

    private String bucketName;
    private String userName;
    private String password;

    /**
     * 默认的超时时间：30秒
     */
    private int timeout = 30;
    /**
     * 默认为自动识别接入点
     */
    private String apiDomain = RestManager.ED_AUTO;

}
