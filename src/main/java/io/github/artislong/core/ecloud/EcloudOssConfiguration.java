package io.github.artislong.core.ecloud;

import cn.hutool.core.util.StrUtil;
import com.baidubce.services.bos.BosClient;
import io.github.artislong.constant.OssConstant;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author 陈敏
 * @version EcloudOssConfiguration.java, v 1.0 2022/5/26 0:01 chenmin Exp $
 * Created on 2022/5/26
 */
@SpringBootConfiguration
@ConditionalOnClass(BosClient.class)
@EnableConfigurationProperties(EcloudOssProperties.class)
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.ECLOUD + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class EcloudOssConfiguration {
}
