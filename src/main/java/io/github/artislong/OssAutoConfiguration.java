package io.github.artislong;

import cn.hutool.extra.spring.EnableSpringUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * @author 陈敏
 * @version OssAutoConfiguration.java, v 1.0 2023/12/18 23:20 chenmin Exp $
 * Created on 2023/12/18
 */
@EnableSpringUtil
@ComponentScan
@Configuration
@EnableConfigurationProperties(OssProperties.class)
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
public class OssAutoConfiguration {
}
