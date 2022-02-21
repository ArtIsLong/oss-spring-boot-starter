package io.github.artislong.core.local;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.local.model.LocalOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version LocalProperties.java, v 1.1 2022/2/11 15:28 chenmin Exp $
 * Created on 2022/2/11
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.LOCAL)
public class LocalOssProperties extends LocalOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<LocalOssConfig> ossConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfigs.isEmpty()) {
            this.init();
        } else {
            ossConfigs.forEach(LocalOssConfig::init);
        }
    }
}
