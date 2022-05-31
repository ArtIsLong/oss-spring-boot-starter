package io.github.artislong.core.sftp;

import cn.hutool.core.util.StrUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.sftp.model.SftpOssConfig;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version SftpOssProperties.java, v 1.1 2021/11/16 15:32 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + StrUtil.DOT + OssConstant.OssType.SFTP)
public class SftpOssProperties extends SftpOssConfig implements InitializingBean {

    private Boolean enable = false;

    private Map<String, SftpOssConfig> ossConfig = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (ossConfig.isEmpty()) {
            this.init();
        } else {
            ossConfig.values().forEach(SftpOssConfig::init);
        }
    }

}
