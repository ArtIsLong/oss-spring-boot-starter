package io.github.artislong.core.sftp.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.extra.ftp.FtpConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

import java.nio.charset.Charset;

/**
 * @author 陈敏
 * @version SftpOssConfig.java, v 1.1 2022/2/20 9:06 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class SftpOssConfig {

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

    private SftpOssClientConfig clientConfig;

    public void init() {
        basePath = OssPathUtil.valid(basePath);
    }

    public FtpConfig toFtpConfig() {
        FtpConfig ftpConfig = new FtpConfig();
        BeanUtil.copyProperties(this, ftpConfig,
                new CopyOptions().setIgnoreNullValue(true).setIgnoreProperties("basePath", "clientConfig"));
        BeanUtil.copyProperties(this.getClientConfig(), ftpConfig, new CopyOptions().setIgnoreNullValue(true));
        return ftpConfig;
    }

}
