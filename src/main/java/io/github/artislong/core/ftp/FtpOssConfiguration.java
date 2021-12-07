package io.github.artislong.core.ftp;

import cn.hutool.extra.ftp.Ftp;
import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version FtpOssConfiguration.java, v 1.1 2021/11/16 15:29 chenmin Exp $
 * Created on 2021/11/16
 */
@Configuration
@ConditionalOnClass(FTPClient.class)
@EnableConfigurationProperties({FtpOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.FTP)
public class FtpOssConfiguration {

    @Autowired
    private FtpOssProperties ftpOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient ftpOssClient(Ftp ftp) {
        return new FtpOssClient(ftp, ossProperties);
    }

    @Bean
    public Ftp ftp() {
        return new Ftp(ftpOssProperties, ftpOssProperties.getMode());
    }

}
