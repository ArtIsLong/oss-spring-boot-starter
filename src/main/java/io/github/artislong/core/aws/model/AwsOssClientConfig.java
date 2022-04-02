package io.github.artislong.core.aws.model;

import lombok.Data;

/**
 * @author 陈敏
 * @version AwsOssClientConfig.java, v 1.0 2022/4/2 16:05 chenmin Exp $
 * Created on 2022/4/2
 */
@Data
public class AwsOssClientConfig {

    private Boolean accelerateModeEnabled = false;
    private Boolean checksumValidationEnabled = false;
    private Boolean multiRegionEnabled = false;
    private Boolean chunkedEncodingEnabled = false;
    private Boolean pathStyleAccessEnabled = false;
    private Boolean useArnRegionEnabled = false;
    private Boolean fipsEnabled = false;
    private Boolean dualstackEnabled = false;

}
