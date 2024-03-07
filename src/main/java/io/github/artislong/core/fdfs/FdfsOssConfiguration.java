package io.github.artislong.core.fdfs;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.github.tobato.fastdfs.FdfsClientConfig;
import com.github.tobato.fastdfs.domain.conn.ConnectionPoolConfig;
import com.github.tobato.fastdfs.domain.fdfs.DefaultThumbImageConfig;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.fdfs.model.FdfsOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version FdfsOssConfiguration.java, v 1.0 2022/10/12 21:46 chenmin Exp $
 * Created on 2022/10/12
 */
@SpringBootConfiguration
@ConditionalOnClass(FastFileStorageClient.class)
@EnableConfigurationProperties(FdfsOssProperties.class)
@EnableAutoConfiguration(exclude = FdfsClientConfig.class)
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssType.FDFS + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class FdfsOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "fdfsOssClient";

    @Autowired
    private FdfsOssProperties fdfsOssProperties;

    @Bean
    public StandardOssClient fdfsOssClient() {
        Map<String, FdfsOssConfig> fdfsOssConfigMap = fdfsOssProperties.getOssConfig();
        if (fdfsOssConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, fdfsOssClient(fdfsOssProperties));
        } else {
            ConnectionPoolConfig poolConfig = fdfsOssProperties.getPoolConfig();
            DefaultThumbImageConfig thumbImageConfig = fdfsOssProperties.getThumbImageConfig();
            fdfsOssConfigMap.forEach((name, fdfsOssConfig) -> {
                if (ObjectUtil.isEmpty(fdfsOssConfig.getPoolConfig())) {
                    fdfsOssConfig.setPoolConfig(poolConfig);
                }
                if (ObjectUtil.isEmpty(fdfsOssConfig.getThumbImageConfig())) {
                    fdfsOssConfig.setThumbImageConfig(thumbImageConfig);
                }
                SpringUtil.registerBean(name, fdfsOssClient(fdfsOssConfig));
            });
        }
        return null;
    }

    public StandardOssClient fdfsOssClient(FdfsOssConfig fdfsOssConfig) {
        return new FdfsOssClient(null, fdfsOssConfig);
    }

}
