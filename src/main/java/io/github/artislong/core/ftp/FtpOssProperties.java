package io.github.artislong.core.ftp;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.ftp.model.FtpOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version FtpOssProperties.java, v 1.1 2021/11/16 15:29 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.FTP)
public class FtpOssProperties extends FtpOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<FtpOssConfig> ftpOssConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (ftpOssConfigs.isEmpty()) {
            this.valid();
        } else {
            ftpOssConfigs.forEach(FtpOssConfig::valid);
        }
    }
}