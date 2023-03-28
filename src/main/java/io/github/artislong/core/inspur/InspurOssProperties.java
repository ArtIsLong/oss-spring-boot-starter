package io.github.artislong.core.inspur;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.inspur.model.InspurOssConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version InspurOssProperties.java, v 1.0 2022/5/17 13:10 chenmin Exp $
 * Created on 2022/5/17
 */
@Slf4j
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.INSPUR)
public class InspurOssProperties extends InspurOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, InspurOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(InspurOssConfig::init);
        }
    }
}