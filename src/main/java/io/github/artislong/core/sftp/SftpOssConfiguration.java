package io.github.artislong.core.sftp;

import cn.hutool.core.text.CharPool;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.sftp.model.SftpOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * @author 陈敏
 * @version SftpOssConfiguration.java, v 1.1 2021/11/16 15:33 chenmin Exp $
 * Created on 2021/11/16
 */
@SpringBootConfiguration
@ConditionalOnClass(ChannelSftp.class)
@EnableConfigurationProperties({SftpOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.SFTP + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class SftpOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "sftpOssClient";

    @Autowired
    private SftpOssProperties sftpOssProperties;

    @Bean
    public StandardOssClient sftpOssClient() {
        Map<String, SftpOssConfig> sftpOssConfigMap = sftpOssProperties.getOssConfig();
        if (sftpOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, sftpOssClient(sftpOssProperties));
        } else {
            sftpOssConfigMap.forEach((name, sftpOssConfig) -> SpringUtil.registerBean(name, sftpOssClient(sftpOssConfig)));
        }
        return null;
    }

    public StandardOssClient sftpOssClient(SftpOssConfig sftpOssConfig) {
        return new SftpOssClient(sftp(sftpOssConfig), sftpOssConfig);
    }

    public Sftp sftp(SftpOssConfig sftpOssConfig) {
        return new Sftp(sftpOssConfig.toFtpConfig());
    }

}
