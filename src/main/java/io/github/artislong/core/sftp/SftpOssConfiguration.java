package io.github.artislong.core.sftp;

import cn.hutool.core.text.CharPool;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.sftp.model.SftpOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author 陈敏
 * @version SftpOssConfiguration.java, v 1.1 2021/11/16 15:33 chenmin Exp $
 * Created on 2021/11/16
 */
@Configuration
@ConditionalOnClass(ChannelSftp.class)
@EnableConfigurationProperties({SftpOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.SFTP + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class SftpOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "sftpOssClient";

    @Autowired
    private SftpOssProperties sftpOssProperties;

    @PostConstruct
    public void init() {
        List<SftpOssConfig> sftpOssConfigs = sftpOssProperties.getOssConfigs();
        if (sftpOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, sftpOssClient(sftp(sftpOssProperties), sftpOssProperties));
        } else {
            for (int i = 0; i < sftpOssConfigs.size(); i++) {
                SftpOssConfig sftpOssConfig = sftpOssConfigs.get(i);
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), sftpOssClient(sftp(sftpOssConfig), sftpOssConfig));
            }
        }
    }

    public StandardOssClient sftpOssClient(Sftp sftp, SftpOssConfig sftpOssConfig) {
        return new SftpOssClient(sftp, sftpOssConfig);
    }

    public Sftp sftp(SftpOssConfig sftpOssConfig) {
        return new Sftp(sftpOssConfig);
    }

}
