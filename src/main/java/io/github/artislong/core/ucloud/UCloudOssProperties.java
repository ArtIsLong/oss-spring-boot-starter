package io.github.artislong.core.ucloud;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.ucloud.model.UCloudOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version UCloudOssProperties.java, v 1.1 2022/3/7 0:20 chenmin Exp $
 * Created on 2022/3/7
 */
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.UCLOUD)
public class UCloudOssProperties extends UCloudOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, UCloudOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(UCloudOssConfig::init);
        }
    }

}
