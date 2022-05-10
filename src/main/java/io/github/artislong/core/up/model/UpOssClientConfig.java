package io.github.artislong.core.up.model;

import io.github.artislong.core.up.constant.ApiDomain;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author 陈敏
 * @version UpOssClientConfig.java, v 1.0 2022/4/21 10:15 chenmin Exp $
 * Created on 2022/4/21
 */
@Data
@Accessors(chain = true)
public class UpOssClientConfig {

    /**
     * 默认的超时时间：30秒
     */
    private int timeout = 30;
    /**
     * 默认为自动识别接入点
     */
    private ApiDomain apiDomain = ApiDomain.ED_AUTO;

}
