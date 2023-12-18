package io.github.artislong.core.sftp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import io.github.artislong.AbstractOssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.sftp.model.SftpOssConfig;
import io.github.artislong.function.ThreeConsumer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version SftpOssConfiguration.java, v 1.1 2021/11/16 15:33 chenmin Exp $
 * Created on 2021/11/16
 */
@SpringBootConfiguration
@ConditionalOnClass(ChannelSftp.class)
@EnableConfigurationProperties({SftpOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.SFTP + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class SftpAbstractOssConfiguration extends AbstractOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "sftpOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        SftpOssProperties sftpOssProperties = getOssProperties(SftpOssProperties.class, OssConstant.OssType.SFTP);
        Map<String, SftpOssConfig> sftpOssConfigMap = sftpOssProperties.getOssConfig();
        if (sftpOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, SftpOssClient.class, buildBeanProMap(sftpOssProperties));
        } else {
            sftpOssConfigMap.forEach((name, sftpOssConfig) -> consumer.accept(name, SftpOssClient.class, buildBeanProMap(sftpOssConfig)));
        }
    }

    public Map<String, Object> buildBeanProMap(SftpOssConfig sftpOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        Sftp sftp = sftp(sftpOssConfig);
        beanProMap.put("sftp", sftp);
        beanProMap.put("sftpOssConfig", sftpOssConfig);
        return beanProMap;
    }

    public Sftp sftp(SftpOssConfig sftpOssConfig) {
        return new Sftp(sftpOssConfig.toFtpConfig());
    }

}
