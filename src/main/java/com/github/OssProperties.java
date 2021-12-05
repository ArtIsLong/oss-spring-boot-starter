package com.github;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.constant.OssType;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version OssProperties.java, v 1.1 2021/11/5 11:05 chenmin Exp $
 * Created on 2021/11/5
 */
@Data
@ConfigurationProperties(prefix = "oss")
public class OssProperties implements InitializingBean {

    /**
     * 对象存储类型，默认本地存储
     */
    private OssType ossType;

    /**
     * 数据存储路径
     */
    private String basePath;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 根路径未配置时，默认路径为 /
        if (ObjectUtil.isEmpty(basePath)) {
            basePath = StrUtil.SLASH;
        }
        // 将路径分隔符统一转为 /
        basePath = basePath.replaceAll("\\\\", StrUtil.SLASH).replaceAll("//", StrUtil.SLASH);

        // 将配置默认转为绝对路径
        if (!basePath.startsWith(StrUtil.SLASH)) {
            basePath = StrUtil.SLASH + basePath;
        }
        if (!basePath.endsWith(StrUtil.SLASH)) {
            basePath = basePath + StrUtil.SLASH;
        }
    }
}
