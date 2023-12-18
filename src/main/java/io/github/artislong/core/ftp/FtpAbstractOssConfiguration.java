package io.github.artislong.core.ftp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.ftp.Ftp;
import io.github.artislong.AbstractOssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ftp.model.FtpOssClientConfig;
import io.github.artislong.core.ftp.model.FtpOssConfig;
import io.github.artislong.function.ThreeConsumer;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author 陈敏
 * @version FtpOssConfiguration.java, v 1.1 2021/11/16 15:29 chenmin Exp $
 * Created on 2021/11/16
 */
@SpringBootConfiguration
@ConditionalOnClass(FTPClient.class)
@EnableConfigurationProperties({FtpOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.FTP + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class FtpAbstractOssConfiguration extends AbstractOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "ftpOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        FtpOssProperties ftpOssProperties = getOssProperties(FtpOssProperties.class, OssConstant.OssType.FTP);
        Map<String, FtpOssConfig> ftpOssConfigMap = ftpOssProperties.getOssConfig();
        if (ftpOssConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, FtpOssClient.class, buildBeanProMap(ftpOssProperties));
        } else {
            FtpOssClientConfig clientConfig = ftpOssProperties.getClientConfig();
            ftpOssConfigMap.forEach((name, ftpOssConfig) -> {
                if (ObjectUtil.isEmpty(ftpOssConfig.getClientConfig())) {
                    ftpOssConfig.setClientConfig(clientConfig);
                }
                consumer.accept(name, FtpOssClient.class, buildBeanProMap(ftpOssConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(FtpOssConfig ftpOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        Ftp ftp = ftp(ftpOssConfig);
        beanProMap.put("ftp", ftp);
        beanProMap.put("ftpOssConfig", ftpOssConfig);
        return beanProMap;
    }

    public Ftp ftp(FtpOssConfig ftpOssConfig) {
        FtpOssClientConfig clientConfig = Optional.ofNullable(ftpOssConfig.getClientConfig()).orElse(new FtpOssClientConfig());
        Ftp ftp = new Ftp(ftpOssConfig.toFtpConfig(), clientConfig.getMode());
        ftp.setBackToPwd(clientConfig.getBackToPwd());
        return ftp;
    }

}
