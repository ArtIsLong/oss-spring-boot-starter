package io.github.artislong.core.fdfs.model;

import com.github.tobato.fastdfs.domain.conn.ConnectionPoolConfig;
import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.conn.PooledConnectionFactory;
import com.github.tobato.fastdfs.domain.conn.TrackerConnectionManager;
import com.github.tobato.fastdfs.domain.fdfs.DefaultThumbImageConfig;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 陈敏
 * @version FdfsOssConfig.java, v 1.0 2022/10/12 23:12 chenmin Exp $
 * Created on 2022/10/12
 */
@Slf4j
@Data
public class FdfsOssConfig {

    private String basePath;
    private TrackerConnectionManager connectionManager;
    private PooledConnectionFactory connectionFactory;
    private FdfsWebServer webServer;
    private ConnectionPoolConfig poolConfig;
    private DefaultThumbImageConfig thumbImageConfig;

    /**
     * 断点续传参数
     */
    private SliceConfig sliceConfig = new SliceConfig();

    public void init() {
        this.sliceConfig.init();
        basePath = OssPathUtil.valid(basePath);
    }
}
