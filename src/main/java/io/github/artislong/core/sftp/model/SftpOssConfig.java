package io.github.artislong.core.sftp.model;

import cn.hutool.extra.ftp.FtpConfig;
import io.github.artislong.utils.PathUtil;
import lombok.Data;

/**
 * @author 陈敏
 * @version SftpOssConfig.java, v 1.1 2022/2/20 9:06 chenmin Exp $
 * Created on 2022/2/20
 */
@Data
public class SftpOssConfig extends FtpConfig {

    private String basePath;

    public void valid() {
        basePath = PathUtil.valid(basePath);
    }

}
