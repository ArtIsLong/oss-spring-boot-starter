package io.github.artislong.core.ftp;

import cn.hutool.core.text.CharPool;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import io.github.artislong.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version FtpOssProperties.java, v 1.1 2021/11/16 15:29 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.FTP)
public class FtpOssProperties extends FtpConfig {
    /**
     * FTP连接模式,默认被动
     */
    private FtpMode mode = FtpMode.Passive;
    /**
     * 设置执行完操作是否返回当前目录,默认false
     */
    private boolean backToPwd = false;
}
