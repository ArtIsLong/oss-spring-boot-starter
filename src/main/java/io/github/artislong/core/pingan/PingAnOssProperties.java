package io.github.artislong.core.pingan;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.pingan.model.PingAnOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version PingAnOssProperties.java, v 1.1 2022/3/8 10:26 chenmin Exp $
 * Created on 2022/3/8
 */
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.PINGAN)
public class PingAnOssProperties extends PingAnOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, PingAnOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(PingAnOssConfig::init);
        }
    }

}
