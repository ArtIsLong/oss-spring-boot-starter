package io.github.artislong.core.aws;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.aws.model.AwsOssConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version AwsOssProperties.java, v 1.0 2022/4/1 18:04 chenmin Exp $
 * Created on 2022/4/1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.AWS)
public class AwsOssProperties extends AwsOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, AwsOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(AwsOssConfig::init);
        }
    }

}