package io.github.artislong.core.ctyun;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.ctyun.model.CtyunOssConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version CtyunOssProperties.java, v 1.0 2022/5/26 0:00 chenmin Exp $
 * Created on 2022/5/26
 */
@Slf4j
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssType.CTYUN)
public class CtyunOssProperties extends CtyunOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, CtyunOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(CtyunOssConfig::init);
        }
    }
}
