package io.github.artislong.core.sftp;

import cn.hutool.core.text.CharPool;
import cn.hutool.extra.ftp.FtpConfig;
import io.github.artislong.constant.OssConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈敏
 * @version SftpOssProperties.java, v 1.1 2021/11/16 15:32 chenmin Exp $
 * Created on 2021/11/16
 */
@Data
@ConfigurationProperties(OssConstant.OSS + CharPool.DOT + OssConstant.OssType.SFTP)
public class SftpOssProperties extends FtpConfig {
}
