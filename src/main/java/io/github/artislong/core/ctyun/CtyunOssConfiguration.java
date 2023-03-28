package io.github.artislong.core.ctyun;

import cn.hutool.core.util.StrUtil;
import com.baidubce.services.bos.BosClient;
import io.github.artislong.constant.OssConstant;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author 陈敏
 * @version CtyunOssConfiguration.java, v 1.0 2022/5/25 23:59 chenmin Exp $
 * Created on 2022/5/25
 */
@SpringBootConfiguration
@ConditionalOnClass(BosClient.class)
@EnableConfigurationProperties(CtyunOssProperties.class)
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.CTYUN + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class CtyunOssConfiguration {
}
