package io.github.artislong.core.jd;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.jd.model.JdOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version JdOssProperties.java, v 1.1 2021/11/25 10:44 chenmin Exp $
 * Created on 2021/11/25
 */
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.JD)
public class JdOssProperties extends JdOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, JdOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(JdOssConfig::init);
        }
    }
}
