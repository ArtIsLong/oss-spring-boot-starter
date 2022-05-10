package io.github.artislong.core.jdbc.model;

import io.github.artislong.utils.OssPathUtil;
import lombok.Data;

import javax.sql.DataSource;

/**
 * @author 陈敏
 * @version JdbcOssConfig.java, v 1.0 2022/3/11 22:04 chenmin Exp $
 * Created on 2022/3/11
 */
@Data
public class JdbcOssConfig {

    private String basePath;

    /**
     * 系统数据源Bean名称(适用于系统多数据源配置)
     */
    private String dataSourceName;

    /**
     * 对象存储数据源
     */
    private String url;
    private Class<? extends DataSource> type;
    private String driver;
    private String username;
    private String password;

    public void init() {
        basePath = OssPathUtil.valid(basePath);
    }
}
