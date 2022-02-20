package io.github.artislong.core.baidu;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.baidu.model.BaiduOssConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version BaiduProperties.java, v 1.1 2021/11/24 15:26 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.BAIDU)
public class BaiduOssProperties extends BaiduOssConfig implements InitializingBean {

    private Boolean enable = false;

    private List<BaiduOssConfig> baiduOssConfigs = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        if (baiduOssConfigs.isEmpty()) {
            this.init();
        } else {
            baiduOssConfigs.forEach(BaiduOssConfig::init);
        }
    }
}
