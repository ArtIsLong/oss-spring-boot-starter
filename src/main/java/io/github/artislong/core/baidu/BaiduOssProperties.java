package io.github.artislong.core.baidu;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.baidu.model.BaiduOssConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version BaiduProperties.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.BAIDU)
public class BaiduOssProperties extends BaiduOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, BaiduOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(BaiduOssConfig::init);
        }
    }
}
