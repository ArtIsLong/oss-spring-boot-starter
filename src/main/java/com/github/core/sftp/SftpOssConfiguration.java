package com.github.core.sftp;

import cn.hutool.extra.ssh.Sftp;
import com.github.OssProperties;
import com.github.constant.OssConstant;
import com.github.core.StandardOssClient;
import com.jcraft.jsch.ChannelSftp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version SftpOssConfiguration.java, v 1.1 2021/11/16 15:33 chenmin Exp $
 * Created on 2021/11/16
 */
@Configuration
@ConditionalOnClass(ChannelSftp.class)
@EnableConfigurationProperties({SftpOssProperties.class, OssProperties.class})
@ConditionalOnProperty(prefix = "oss", name = "oss-type", havingValue = OssConstant.OssType.SFTP)
public class SftpOssConfiguration {

    @Autowired
    private SftpOssProperties sftpOssProperties;
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public StandardOssClient sftpOssClient(Sftp sftp) {
        return new SftpOssClient(sftp, ossProperties);
    }

    @Bean
    public Sftp sftp() {
        return new Sftp(sftpOssProperties);
    }

}
