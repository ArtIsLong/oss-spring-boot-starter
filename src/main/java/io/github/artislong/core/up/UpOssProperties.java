package io.github.artislong.core.up;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.up.model.UpOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version UpOssProperties.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.UP)
public class UpOssProperties extends UpOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<UpOssConfig> upOssConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (upOssConfigs.isEmpty()) {
            this.valid();
        } else {
            upOssConfigs.forEach(UpOssConfig::valid);
        }
    }

}
