package io.github.artislong.core.tencent;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.tencent.model.TencentOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version TencentProperties.java, v 1.1 2021/11/24 15:22 chenmin Exp $
 * Created on 2021/11/24
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.TENCENT)
public class TencentOssProperties extends TencentOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<TencentOssConfig> tencentOssConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (tencentOssConfigs.isEmpty()) {
            this.valid();
        } else {
            tencentOssConfigs.forEach(TencentOssConfig::valid);
        }
    }

}
