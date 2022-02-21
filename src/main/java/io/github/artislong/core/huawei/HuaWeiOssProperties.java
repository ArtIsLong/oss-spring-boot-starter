package io.github.artislong.core.huawei;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.huawei.model.HuaweiOssConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version HuaWeiOssProperties.java, v 1.1 2021/11/25 9:56 chenmin Exp $
 * Created on 2021/11/25
 */
@Slf4j
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.HUAWEI)
public class HuaWeiOssProperties extends HuaweiOssConfig implements InitializingBean {

    private Boolean enable = false;
    
    private List<HuaweiOssConfig> ossConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfigs.isEmpty()) {
            this.init();
        } else {
            ossConfigs.forEach(HuaweiOssConfig::init);
        }
    }
}
