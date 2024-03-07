package io.github.artislong.core.up;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.up.model.UpOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version UpOssProperties.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssType.UP)
public class UpOssProperties extends UpOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, UpOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(UpOssConfig::init);
        }
    }

}
