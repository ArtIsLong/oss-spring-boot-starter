package io.github.artislong;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version OssProperties.java, v 1.1 2021/11/5 11:05 chenmin Exp $
 * Created on 2021/11/5
 */
@Data
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

}
