package io.github.artislong.core.sftp.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author 陈敏
 * @version SftpOssClientConfig.java, v 1.0 2022/5/9 0:27 chenmin Exp $
 * Created on 2022/5/9
 */
@Data
@Accessors(chain = true)
public class SftpOssClientConfig {
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
