package io.github.artislong.core.local;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.local.model.LocalOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version LocalProperties.java, v 1.1 2022/2/11 15:28 chenmin Exp $
 * Created on 2022/2/11
 */
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssType.LOCAL)
public class LocalOssProperties extends LocalOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, LocalOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(LocalOssConfig::init);
        }
    }
}
