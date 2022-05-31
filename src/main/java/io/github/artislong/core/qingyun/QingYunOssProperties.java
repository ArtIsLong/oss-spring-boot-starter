package io.github.artislong.core.qingyun;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.qingyun.model.QingYunOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version QingYunOssProperties.java, v 1.0 2022/3/10 23:52 chenmin Exp $
 * Created on 2022/3/10
 */
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.QINGYUN)
public class QingYunOssProperties extends QingYunOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, QingYunOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(QingYunOssConfig::init);
        }
    }

}