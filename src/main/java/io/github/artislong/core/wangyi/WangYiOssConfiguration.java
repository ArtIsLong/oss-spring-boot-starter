/** * $Id: WangYiOssConfiguration.java,v 1.0 2022/3/4 9:49 PM chenmin Exp $ */package io.github.artislong.core.wangyi;import cn.hutool.core.util.StrUtil;import cn.hutool.core.util.ObjectUtil;import com.netease.cloud.auth.BasicCredentials;import com.netease.cloud.auth.Credentials;import com.netease.cloud.services.nos.NosClient;import io.github.artislong.OssConfiguration;import io.github.artislong.constant.OssConstant;import io.github.artislong.constant.OssType;import io.github.artislong.core.StandardOssClient;import io.github.artislong.core.wangyi.model.WangYiOssClientConfig;import io.github.artislong.core.wangyi.model.WangYiOssConfig;import io.github.artislong.function.ThreeConsumer;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.boot.context.properties.EnableConfigurationProperties;import java.util.HashMap;import java.util.Map;/** * @author 陈敏 * @version $Id: WangYiOssConfiguration.java,v 1.1 2022/3/4 9:49 PM chenmin Exp $ * Created on 2022/3/4 9:49 PM * My blog： https://www.chenmin.info */@SpringBootConfiguration@ConditionalOnClass(NosClient.class)@EnableConfigurationProperties({WangYiOssProperties.class})@ConditionalOnProperty(prefix = OssConstant.OSS, name = OssType.WANGYI + StrUtil.DOT + OssConstant.ENABLE,        havingValue = OssConstant.DEFAULT_ENABLE_VALUE)public class WangYiOssConfiguration extends OssConfiguration {    public static final String DEFAULT_BEAN_NAME = "wangYiOssClient";    @Override    public void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer) {        WangYiOssProperties wangYiOssProperties = getOssProperties(WangYiOssProperties.class, OssType.WANGYI);        Map<String, WangYiOssConfig> wangYiOssConfigMap = wangYiOssProperties.getOssConfig();        if (wangYiOssConfigMap.isEmpty()) {            consumer.accept(DEFAULT_BEAN_NAME, WangYiOssClient.class, buildBeanProMap(wangYiOssProperties));        } else {            String endPoint = wangYiOssProperties.getEndpoint();            String accessKey = wangYiOssProperties.getAccessKey();            String secretKey = wangYiOssProperties.getSecretKey();            WangYiOssClientConfig clientConfig = wangYiOssProperties.getClientConfig();            wangYiOssConfigMap.forEach((name, wangYiOssConfig) -> {                if (ObjectUtil.isEmpty(wangYiOssConfig.getEndpoint())) {                    wangYiOssConfig.setEndpoint(endPoint);                }                if (ObjectUtil.isEmpty(wangYiOssConfig.getAccessKey())) {                    wangYiOssConfig.setAccessKey(accessKey);                }                if (ObjectUtil.isEmpty(wangYiOssConfig.getSecretKey())) {                    wangYiOssConfig.setSecretKey(secretKey);                }                if (ObjectUtil.isEmpty(wangYiOssConfig.getClientConfig())) {                    wangYiOssConfig.setClientConfig(clientConfig);                }                consumer.accept(name, WangYiOssClient.class, buildBeanProMap(wangYiOssConfig));            });        }    }    public Map<String, Object> buildBeanProMap(WangYiOssConfig wangYiOssConfig) {        Map<String, Object> beanProMap = new HashMap<>();        NosClient nosClient = nosClient(wangYiOssConfig);        beanProMap.put("nosClient", nosClient);        beanProMap.put("wangYiOssConfig", wangYiOssConfig);        return beanProMap;    }    public NosClient nosClient(WangYiOssConfig ossConfig) {        Credentials credentials = new BasicCredentials(ossConfig.getAccessKey(), ossConfig.getSecretKey());        NosClient nosClient = new NosClient(credentials);        nosClient.setEndpoint(ossConfig.getEndpoint());        nosClient.setConfiguration(ossConfig.getClientConfig().toClientConfig());        return nosClient;    }}