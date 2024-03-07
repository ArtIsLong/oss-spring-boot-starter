package io.github.artislong.core.qiniu;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.qiniu.model.QiNiuOssConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version QiNiuOssProperties.java, v 1.1 2021/11/16 15:30 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssType.QINIU)
public class QiNiuOssProperties extends QiNiuOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, QiNiuOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(QiNiuOssConfig::init);
        }
    }

}
