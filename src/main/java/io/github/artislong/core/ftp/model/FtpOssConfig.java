package io.github.artislong.core.ftp.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.extra.ftp.FtpConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

/**
 * @author 陈敏
 * @version FtpOssConfig.java, v 1.1 2022/2/19 18:29 chenmin Exp $
 * Created on 2022/2/19
 */
@Slf4j
@Data
public class FtpOssConfig {

    private String basePath;

    /**
     * 主机
     */
    private String host;
    /**
     * 端口
     */
    private int port;
    /**
     * 用户名
     */
    private String user;
    /**
     * 密码
     */
    private String password;
    /**
     * 编码
     */
    private Charset charset;

    private FtpOssClientConfig clientConfig;

    public void init() {
        basePath = OssPathUtil.valid(basePath);
    }

    public FtpConfig toFtpConfig() {
        FtpConfig ftpConfig = new FtpConfig();
        BeanUtil.copyProperties(this, ftpConfig,
                new CopyOptions().setIgnoreNullValue(true).setIgnoreProperties("basePath", "clientConfig"));
        BeanUtil.copyProperties(this.getClientConfig(), ftpConfig,
                new CopyOptions().setIgnoreNullValue(true).setIgnoreProperties("mode", "backToPwd"));
        return ftpConfig;
    }

}
