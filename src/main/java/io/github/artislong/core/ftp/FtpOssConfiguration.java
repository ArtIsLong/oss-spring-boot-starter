package io.github.artislong.core.ftp;

import cn.hutool.core.text.CharPool;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.spring.SpringUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ftp.model.FtpOssConfig;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author 陈敏
 * @version FtpOssConfiguration.java, v 1.1 2021/11/16 15:29 chenmin Exp $
 * Created on 2021/11/16
 */
@Configuration
@ConditionalOnClass(FTPClient.class)
@EnableConfigurationProperties({FtpOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.FTP + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class FtpOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "ftpOssClient";

    @Autowired
    private FtpOssProperties ftpOssProperties;

    @Bean
    public StandardOssClient ftpOssClient() {
        Map<String, FtpOssConfig> ftpOssConfigMap = ftpOssProperties.getOssConfig();
        if (ftpOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, ftpOssClient(ftpOssProperties));
        } else {
            ftpOssConfigMap.forEach((name, ftpOssConfig) -> SpringUtil.registerBean(name, ftpOssClient(ftpOssConfig)));
        }
        return null;
    }

    public StandardOssClient ftpOssClient(FtpOssConfig ftpOssConfig) {
        return new FtpOssClient(ftp(ftpOssConfig), ftpOssConfig);
    }

    public Ftp ftp(FtpOssConfig ftpOssConfig) {
        Ftp ftp = new Ftp(ftpOssConfig, ftpOssConfig.getMode());
        ftp.setBackToPwd(ftpOssConfig.isBackToPwd());
        return ftp;
    }

}
