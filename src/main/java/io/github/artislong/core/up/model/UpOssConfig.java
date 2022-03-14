package io.github.artislong.core.up.model;

import io.github.artislong.core.up.constant.ApiDomain;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version UpOssConfig.java, v 1.1 2022/2/20 9:13 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class UpOssConfig {

    private String basePath;
    private String bucketName;
    private String userName;
    private String password;

    /**
     * 默认的超时时间：30秒
     */
    private int timeout = 30;
    /**
     * 默认为自动识别接入点
     */
    private ApiDomain apiDomain = ApiDomain.ED_AUTO;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
