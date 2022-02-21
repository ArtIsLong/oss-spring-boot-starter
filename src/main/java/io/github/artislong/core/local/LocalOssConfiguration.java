package io.github.artislong.core.local;

import cn.hutool.core.text.CharPool;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSSClient;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.local.model.LocalOssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author 陈敏
 * @version LocalConfiguration.java, v 1.1 2022/2/11 15:28 chenmin Exp $
 * Created on 2022/2/11
 */
@Configuration
@ConditionalOnClass(OSSClient.class)
@EnableConfigurationProperties({LocalOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.LOCAL + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class LocalOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "localOssClient";

    @Autowired
    private LocalOssProperties localProperties;

    @PostConstruct
    public void init() {
        List<LocalOssConfig> localOssConfigs = localProperties.getOssConfigs();
        if (localOssConfigs.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, localOssClient(localProperties));
        } else {
            for (int i = 0; i < localOssConfigs.size(); i++) {
                LocalOssConfig localOssConfig = localOssConfigs.get(i);
                SpringUtil.registerBean(DEFAULT_BEAN_NAME + (i + 1), localOssClient(localOssConfig));
            }
        }
    }

    public StandardOssClient localOssClient(LocalOssConfig localOssConfig) {
        return new LocalOssClient(localOssConfig);
    }
}
