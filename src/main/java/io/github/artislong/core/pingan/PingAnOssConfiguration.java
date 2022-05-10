package io.github.artislong.core.pingan;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.pingan.radosgw.sdk.RGWPassport;
import com.pingan.radosgw.sdk.admin.RadosgwAdminServiceFactory;
import com.pingan.radosgw.sdk.admin.service.RGWAdminServiceFacade;
import com.pingan.radosgw.sdk.config.ObsClientConfig;
import com.pingan.radosgw.sdk.service.RadosgwService;
import com.pingan.radosgw.sdk.service.RadosgwServiceFactory;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.pingan.model.PingAnOssConfig;
import io.github.artislong.exception.OssException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import repkg.com.amazonaws.AmazonClientException;

import java.util.Map;

/**
 * @author 陈敏
 * @version PingAnOssConfiguration.java, v 1.1 2022/3/8 10:25 chenmin Exp $
 * Created on 2022/3/8
 */
@SpringBootConfiguration
@ConditionalOnClass(RadosgwService.class)
@EnableConfigurationProperties({PingAnOssProperties.class})
@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssConstant.OssType.PINGAN + CharPool.DOT + OssConstant.ENABLE,
        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)
public class PingAnOssConfiguration {

    public static final String DEFAULT_BEAN_NAME = "pingAnOssClient";

    @Autowired
    private PingAnOssProperties pingAnOssProperties;

    @Bean
    public StandardOssClient pingAnOssClient() {
        Map<String, PingAnOssConfig> ossConfigMap = pingAnOssProperties.getOssConfig();
        if (ossConfigMap.isEmpty()) {
            SpringUtil.registerBean(DEFAULT_BEAN_NAME, pingAnOssClient(pingAnOssProperties));
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
                SpringUtil.registerBean(name, pingAnOssClient(ossConfig));
            });
        }
        return null;
    }

    public StandardOssClient pingAnOssClient(PingAnOssConfig pingAnOssConfig) {
        return new PingAnOssClient(pingAnOssConfig, radosgwService(pingAnOssConfig), rgwAdminServiceFacade(pingAnOssConfig));
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
