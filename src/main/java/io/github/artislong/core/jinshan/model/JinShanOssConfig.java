package io.github.artislong.core.jinshan.model;

import com.ksyun.ks3.http.Region;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version JinShanOssConfig.java, v 1.1 2022/3/3 22:28 chenmin Exp $
 * Created on 2022/3/3
 */
@Data
public class JinShanOssConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String endpoint;
    /**
     * 服务地址,参考{@linkplain http://ks3.ksyun.com/doc/api/index.html Doc} </br>
     * 中国（杭州）:kss.ksyun.com</br>
     * 中国（杭州）cdn:kssws.ks-cdn.com</br>
     * 美国（圣克拉拉）:ks3-us-west-1.ksyun.com</br>
     * 中国（北京）:ks3-cn-beijing.ksyun.com</br>
     * 中国（香港）:ks3-cn-hk-1.ksyun.com</br>
     * 中国（上海）:ks3-cn-shanghai.ksyun.com</br>
     */
    private Region region = null;
    private String securityToken;
    private String basePath;

    private JinShanOssClientConfig clientConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }

}
