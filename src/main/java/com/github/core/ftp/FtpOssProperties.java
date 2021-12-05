package com.github.core.ftp;

import cn.hutool.core.text.CharPool;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import com.github.constant.OssConstant;
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
    private FtpMode mode;
    private boolean backToPwd;
}
