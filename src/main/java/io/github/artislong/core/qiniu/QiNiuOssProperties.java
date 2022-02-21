package io.github.artislong.core.qiniu;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.qiniu.model.QiNiuOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version QiNiuOssProperties.java, v 1.1 2021/11/16 15:30 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.QINIU)
public class QiNiuOssProperties extends QiNiuOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<QiNiuOssConfig> ossConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfigs.isEmpty()) {
            this.init();
        } else {
            ossConfigs.forEach(QiNiuOssConfig::init);
        }
    }

}
