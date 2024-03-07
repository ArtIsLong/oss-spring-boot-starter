package io.github.artislong.core.pingan;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.pingan.radosgw.sdk.RGWPassport;
import com.pingan.radosgw.sdk.admin.RadosgwAdminServiceFactory;
import com.pingan.radosgw.sdk.admin.service.RGWAdminServiceFacade;
import com.pingan.radosgw.sdk.config.ObsClientConfig;
import com.pingan.radosgw.sdk.service.RadosgwService;
import com.pingan.radosgw.sdk.service.RadosgwServiceFactory;
import io.github.artislong.OssConfiguration;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.pingan.model.PingAnOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.function.ThreeConsumer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import repkg.com.amazonaws.AmazonClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈敏
 * @version PingAnOssConfiguration.java, v 1.1 2022/3/8 10:25 chenmin Exp $
 * Created on 2022/3/8
 */
@SpringBootConfiguration
@ConditionalOnClass(RadosgwService.class)
@EnableConfigurationProperties({PingAnOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssType.PINGAN + StrUtil.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class PingAnOssConfiguration extends OssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "pingAnOssClient";

    @Override
    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {
        PingAnOssProperties pingAnOssProperties = getOssProperties(PingAnOssProperties.class, OssType.PINGAN);
        Map<String, PingAnOssConfig> ossConfigMap = pingAnOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            consumer.accept(DEFAULT_BEAN_NAME, PingAnOssClient.class, buildBeanProMap(pingAnOssProperties));
        } else {
            String userAgent = pingAnOssProperties.getUserAgent();
            String obsUrl = pingAnOssProperties.getObsUrl();
            String obsAccessKey = pingAnOssProperties.getObsAccessKey();
            String obsSecret = pingAnOssProperties.getObsSecret();
            String domainName = pingAnOssProperties.getDomainName();
            ossConfigMap.forEach((name, ossConfig) -> {
                if (ObjectUtil.isEmpty(ossConfig.getUserAgent())) {
                    ossConfig.setUserAgent(userAgent);
                }
                if (ObjectUtil.isEmpty(ossConfig.getObsUrl())) {
                    ossConfig.setObsUrl(obsUrl);
                }
                if (ObjectUtil.isEmpty(ossConfig.getObsAccessKey())) {
                    ossConfig.setObsAccessKey(obsAccessKey);
                }
                if (ObjectUtil.isEmpty(ossConfig.getObsSecret())) {
                    ossConfig.setObsSecret(obsSecret);
                }
                if (ObjectUtil.isEmpty(ossConfig.getDomainName())) {
                    ossConfig.setDomainName(domainName);
                }
                consumer.accept(name, PingAnOssClient.class, buildBeanProMap(ossConfig));
            });
        }
    }

    public Map<String, Object> buildBeanProMap(PingAnOssConfig pingAnOssConfig) {
        Map<String, Object> beanProMap = new HashMap<>();
        RadosgwService radosgwService = radosgwService(pingAnOssConfig);
        RGWAdminServiceFacade rgwAdminServiceFacade = rgwAdminServiceFacade(pingAnOssConfig);
        beanProMap.put("pingAnOssConfig", pingAnOssConfig);
        beanProMap.put("radosgwService", radosgwService);
        beanProMap.put("rgwAdminServiceFacade", rgwAdminServiceFacade);
        return beanProMap;
    }

    public RGWAdminServiceFacade rgwAdminServiceFacade(PingAnOssConfig pingAnOssConfig) {
        ObsClientConfig obsClientConfig = obsClientConfig(pingAnOssConfig);
        try {
            RGWPassport rgwPassport = new RGWPassport(obsClientConfig);
            return RadosgwAdminServiceFactory.get(rgwPassport);
        } catch (AmazonClientException e) {
            throw new OssException(e);
        }
    }

    public ObsClientConfig obsClientConfig(PingAnOssConfig pingAnOssConfig) {
        return new ObsClientConfig() {
            @Override
            public String getUserAgent() {
                return pingAnOssConfig.getUserAgent();
            }

            @Override
            public String getObsUrl() {
                return pingAnOssConfig.getObsUrl();
            }

            @Override
            public String getObsAccessKey() {
                return pingAnOssConfig.getObsAccessKey();
            }

            @Override
            public String getObsSecret() {
                return pingAnOssConfig.getObsSecret();
            }

            @Override
            public boolean isRepresentPathInKey() {
                return pingAnOssConfig.getRepresentPathInKey();
            }
        };
    }

    public RadosgwService radosgwService(PingAnOssConfig pingAnOssConfig) {
        ObsClientConfig obsClientConfig = obsClientConfig(pingAnOssConfig);
        try {
            String domainName = pingAnOssConfig.getDomainName();
            if (ObjectUtil.isEmpty(domainName)) {
                return RadosgwServiceFactory.getFromConfigObject(obsClientConfig);
            } else {
                return RadosgwServiceFactory.getFromConfigObject(obsClientConfig, domainName);
            }
        } catch (Exception e) {
            throw new OssException(e);
        }
    }
}
