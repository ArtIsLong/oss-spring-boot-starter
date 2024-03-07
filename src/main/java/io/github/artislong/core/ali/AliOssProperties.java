package io.github.artislong.core.ali;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.ali.model.AliOssConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version AliOssProperties.java, v 1.1 2021/11/16 15:25 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(OssConstant.OSS + StrPool.DOT + OssType.ALI)
public class AliOssProperties extends AliOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, AliOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(AliOssConfig::init);
        }
    }

}
