package io.github.artislong.core.local;

import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSSClient;
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
@ConditionalOnProperty(prefix = "oss", name = "local", havingValue = "true")
public class LocalOssConfiguration {

    @Autowired
    private LocalOssProperties localProperties;

    @PostConstruct
    public void init() {
        final String defaultBeanName = "localOssClient";
        List<LocalOssConfig> localOssConfigs = localProperties.getLocalOssConfigs();
        if (localOssConfigs.isEmpty()) {
            SpringUtil.registerBean(defaultBeanName, localOssClient(localProperties));
        } else {
            for (int i = 0; i < localOssConfigs.size(); i++) {
                LocalOssConfig localOssConfig = localOssConfigs.get(i);
                SpringUtil.registerBean(defaultBeanName + (i + 1), localOssClient(localOssConfig));
            }
        }
    }

    public StandardOssClient localOssClient(LocalOssConfig localOssConfig) {
        return new LocalOssClient(localOssConfig);
    }
}
