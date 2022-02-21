package io.github.artislong.core.jd;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.jd.model.JdOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version JdOssProperties.java, v 1.1 2021/11/25 10:44 chenmin Exp $
 * Created on 2021/11/25
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.JD)
public class JdOssProperties extends JdOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<JdOssConfig> ossConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfigs.isEmpty()) {
            this.init();
        } else {
            ossConfigs.forEach(JdOssConfig::init);
        }
    }
}
