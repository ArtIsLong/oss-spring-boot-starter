package io.github.artislong;

import cn.hutool.extra.spring.EnableSpringUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈敏
 * @version OssAutoConfiguration.java, v 1.1 2021/11/5 11:05 chenmin Exp $
 * Created on 2021/11/5
 */
@EnableSpringUtil
@Configuration
@ComponentScan(basePackages = "io.github.artislong")
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

}
