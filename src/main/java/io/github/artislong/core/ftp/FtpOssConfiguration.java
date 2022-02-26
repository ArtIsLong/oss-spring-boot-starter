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

import javax.annotation.PostConstruct;
import java.util.List;

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
    public void init() {
        List<FtpOssConfig> ftpOssConfigs = ftpOssProperties.getOssConfigs();
        if (ftpOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, ftpOssClient(ftp(ftpOssProperties), ftpOssProperties));
        } else {
            for (int i = 0; i < ftpOssConfigs.size(); i++) {
                FtpOssConfig ftpOssConfig = ftpOssConfigs.get(i);
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), ftpOssClient(ftp(ftpOssConfig), ftpOssConfig));
            }
        }
    }

    public StandardOssClient ftpOssClient(Ftp ftp, FtpOssConfig ftpOssConfig) {
        return new FtpOssClient(ftp, ftpOssConfig);
    }

    public Ftp ftp(FtpOssConfig ftpOssConfig) {
        Ftp ftp = new Ftp(ftpOssConfig, ftpOssConfig.getMode());
        ftp.setBackToPwd(ftpOssConfig.isBackToPwd());
        return ftp;
    }

}
