package io.github.artislong.core.ftp.model;

import cn.hutool.extra.ftp.FtpMode;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author 陈敏
 * @version FtpOssClientConfig.java, v 1.0 2022/4/25 17:26 chenmin Exp $
 * Created on 2022/4/25
 */
@Data
@Accessors(chain = true)
public class FtpOssClientConfig {
    /**
     * FTP连接模式,默认被动
     */
    private FtpMode mode = FtpMode.Passive;
    /**
     * 设置执行完操作是否返回当前目录,默认false
     */
    private boolean backToPwd = false;
    /**
     * 连接超时时长，单位毫秒
     */
    private long connectionTimeout;
    /**
     * Socket连接超时时长，单位毫秒
     */
    private long soTimeout;
    /**
     * 设置服务器语言
     */
    private String serverLanguageCode;
    /**
     * 设置服务器系统关键词
     */
    private String systemKey;

}
