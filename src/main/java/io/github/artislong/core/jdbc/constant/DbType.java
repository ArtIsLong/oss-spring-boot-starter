package io.github.artislong.core.jdbc.constant;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.exception.OssException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库类型
 * @author 陈敏
 * @version DbType.java, v 1.0 2023/4/16 22:57 chenmin Exp $
 * Created on 2023/4/16
 */
@Slf4j
@Getter
public enum DbType {
    /**
     * MYSQL
     */
    MYSQL("mysql", "%s LIKE CONCAT('%%',#{%s},'%%')", "MySql数据库"),
    /**
     * MARIADB
     */
    MARIADB("mariadb", "%s LIKE CONCAT('%%',#{%s},'%%')", "MariaDB数据库"),
    /**
     * ORACLE
     */
    ORACLE("oracle", "%s LIKE CONCAT(CONCAT('%%',#{%s}),'%%')", "Oracle数据库"),
    /**
     * DB2
     */
    DB2("db2", "%s LIKE CONCAT(CONCAT('%%',#{%s}),'%%')", "DB2数据库"),
    /**
     * H2
     */
    H2("h2", "%s LIKE CONCAT('%%',#{%s},'%%')", "H2数据库"),
    /**
     * HSQL
     */
    HSQL("hsql", "%s LIKE CONCAT('%%',#{%s},'%%')", "HSQL数据库"),
    /**
     * SQLITE
     */
    SQLITE("sqlite", "%s LIKE CONCAT('%%',#{%s},'%%')", "SQLite数据库"),
    /**
     * POSTGRE
     */
    POSTGRE_SQL("postgresql", "%s LIKE CONCAT('%%',#{%s},'%%')", "Postgre数据库"),
    /**
     * SQLSERVER2005
     */
    SQL_SERVER2005("sqlserver2005", "%s LIKE '%%'+#{%s}+'%%'", "SQLServer2005数据库"),
    /**
     * SQLSERVER
     */
    SQL_SERVER("sqlserver", "%s LIKE '%%'+#{%s}+'%%'", "SQLServer数据库"),
    /**
     * DM
     */
    DM("dm", null, "达梦数据库"),
    /**
     * UNKONWN DB
     */
    OTHER("other", null, "其他数据库");

    /**
     * 数据库名称
     */
    private final String db;
    /**
     * LIKE 拼接模式
     */
    private final String like;
    /**
     * 描述
     */
    private final String desc;


    DbType(String db, String like, String desc) {
        this.db = db;
        this.like = like;
        this.desc = desc;
    }

    /**
     * 获取数据库类型（默认 MySql）
     *
     * @param dbType 数据库类型字符串
     */
    public static DbType toDbType(String dbType) {
        DbType[] dts = DbType.values();
        for (DbType dt : dts) {
            if (dt.getDb().equalsIgnoreCase(dbType)) {
                return dt;
            }
        }
        return OTHER;
    }

    /**
     * 根据连接地址判断数据库类型
     *
     * @param jdbcUrl 连接地址
     * @return ignore
     */
    public static DbType getDbType(String jdbcUrl) {
        Assert.isFalse(StrUtil.isEmpty(jdbcUrl), "Error: The jdbcUrl is Null, Cannot read database type");
        if (jdbcUrl.startsWith("jdbc:mysql:") || jdbcUrl.startsWith("jdbc:cobar:")
                || jdbcUrl.startsWith("jdbc:log4jdbc:mysql:")) {
            return DbType.MYSQL;
        } else if (jdbcUrl.startsWith("jdbc:mariadb:")) {
            return DbType.MARIADB;
        } else if (jdbcUrl.startsWith("jdbc:oracle:") || jdbcUrl.startsWith("jdbc:log4jdbc:oracle:")) {
            return DbType.ORACLE;
        } else if (jdbcUrl.startsWith("jdbc:sqlserver:") || jdbcUrl.startsWith("jdbc:microsoft:")) {
            return DbType.SQL_SERVER2005;
        } else if (jdbcUrl.startsWith("jdbc:sqlserver2012:")) {
            return DbType.SQL_SERVER;
        } else if (jdbcUrl.startsWith("jdbc:postgresql:") || jdbcUrl.startsWith("jdbc:log4jdbc:postgresql:")) {
            return DbType.POSTGRE_SQL;
        } else if (jdbcUrl.startsWith("jdbc:hsqldb:") || jdbcUrl.startsWith("jdbc:log4jdbc:hsqldb:")) {
            return DbType.HSQL;
        } else if (jdbcUrl.startsWith("jdbc:db2:")) {
            return DbType.DB2;
        } else if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            return DbType.SQLITE;
        } else if (jdbcUrl.startsWith("jdbc:h2:") || jdbcUrl.startsWith("jdbc:log4jdbc:h2:")) {
            return DbType.H2;
        } else if (jdbcUrl.startsWith("jdbc:dm:") || jdbcUrl.startsWith("jdbc:log4jdbc:dm:")) {
            return DbType.DM;
        } else {
            log.warn("The jdbcUrl is " + jdbcUrl + ", Oss Cannot Read Database type or The Database's Not Supported!");
            return DbType.OTHER;
        }
    }

    /**
     * 根据连接池判断数据库类型
     *
     * @param dataSource 连接池
     * @return ignore
     */
    public static DbType getDbType(DataSource dataSource) {
        String databaseProductName;
        try (Connection connection = dataSource.getConnection()) {
            databaseProductName = connection.getMetaData().getDatabaseProductName();
//            databaseProductName = connection.getMetaData().getDriverName();
            if (StrUtil.containsIgnoreCase(databaseProductName, "mysql") || StrUtil.containsIgnoreCase(databaseProductName, "cobar")) {
                return DbType.MYSQL;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "mariadb")) {
                return DbType.MARIADB;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "oracle")) {
                return DbType.ORACLE;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "sqlserver") || StrUtil.containsIgnoreCase(databaseProductName, "microsoft")) {
                return DbType.SQL_SERVER2005;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "sqlserver2012")) {
                return DbType.SQL_SERVER;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "postgresql")) {
                return DbType.POSTGRE_SQL;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "hsqldb")) {
                return DbType.HSQL;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "db2")) {
                return DbType.DB2;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "sqlite")) {
                return DbType.SQLITE;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "h2")) {
                return DbType.H2;
            } else if (StrUtil.containsIgnoreCase(databaseProductName, "dm")) {
                return DbType.DM;
            } else {
                log.warn("The databaseProductName is " + databaseProductName + ", Oss Cannot Read Database type or The Database's Not Supported!");
                return DbType.OTHER;
            }
        } catch (Exception e) {
            throw new OssException("数据源连接失败，请检查配置!");
        }
    }
}