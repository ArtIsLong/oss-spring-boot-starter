package io.github.artislong.core.ali;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.ali.model.AliOssConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version AliOssProperties.java, v 1.1 2021/11/16 15:25 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.ALI)
public class AliOssProperties extends AliOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<AliOssConfig> ossConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfigs.isEmpty()) {
            this.init();
        } else {
            ossConfigs.forEach(AliOssConfig::init);
        }
    }

}
