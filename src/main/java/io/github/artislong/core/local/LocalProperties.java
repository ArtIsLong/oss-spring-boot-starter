package io.github.artislong.core.local;

import cn.hutool.core.text.CharPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.model.SliceConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version LocalProperties.java, v 1.1 2022/2/11 15:28 chenmin Exp $
 * Created on 2022/2/11
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.LOCAL)
public class LocalProperties implements InitializingBean {

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    @Override
    public void afterPropertiesSet() {
        this.getSliceConfig().valid();
    }
}
