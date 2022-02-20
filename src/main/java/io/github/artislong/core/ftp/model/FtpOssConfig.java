package io.github.artislong.core.ftp.model;

import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import io.github.artislong.utils.PathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 陈敏
 * @version FtpOssConfig.java, v 1.1 2022/2/19 18:29 chenmin Exp $
 * Created on 2022/2/19
 */
@Slf4j
@Data
public class FtpOssConfig extends FtpConfig {

    private String basePath;
    /**
     * FTP连接模式,默认被动
     */
    private FtpMode mode = FtpMode.Passive;
    /**
     * 设置执行完操作是否返回当前目录,默认false
     */
    private boolean backToPwd = false;

    public void valid() {
        basePath = PathUtil.valid(basePath);
    }

}
