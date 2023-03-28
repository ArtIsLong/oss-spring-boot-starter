package io.github.artislong.core.fdfs;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.fdfs.model.FdfsOssConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version FdfsOssProperties.java, v 1.0 2022/10/12 21:46 chenmin Exp $
 * Created on 2022/10/12
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.FDFS)
public class FdfsOssProperties extends FdfsOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, FdfsOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(FdfsOssConfig::init);
        }
    }
}