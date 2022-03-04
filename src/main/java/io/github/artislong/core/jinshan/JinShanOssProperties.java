package io.github.artislong.core.jinshan;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.jinshan.model.JinShanOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version JinShanOssProperties.java, v 1.1 2022/3/3 22:10 chenmin Exp $
 * Created on 2022/3/3
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.JINSHAN)
public class JinShanOssProperties extends JinShanOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, JinShanOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(JinShanOssConfig::init);
        }
    }
}
