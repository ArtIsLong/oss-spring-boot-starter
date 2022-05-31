package io.github.artislong.core.jdbc.adapter;

import io.github.artislong.exception.NotSupportException;
import io.github.artislong.exception.OssException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author 陈敏
 * @version JdbcOssOperationFactory.java, v 1.0 2022/5/25 17:56 chenmin Exp $
 * Created on 2022/5/25
 */
public class JdbcOssOperationFactoryBean implements FactoryBean<JdbcOssOperation>, InitializingBean {

    @Setter
    @Getter
    private DataSource dataSource;
    private JdbcOssOperation jdbcOssOperation;

    @Override
    public JdbcOssOperation getObject() throws Exception {
        if (this.jdbcOssOperation == null) {
            afterPropertiesSet();
        }
        return this.jdbcOssOperation;
    }

    @Override
    public Class<?> getObjectType() {
        return this.jdbcOssOperation == null ? JdbcOssOperation.class : this.jdbcOssOperation.getClass();
    }

    @Override
    public void afterPropertiesSet() {
        String driverName;
        try (Connection connection = dataSource.getConnection()) {
            connection.getMetaData().getDatabaseProductName();
            driverName = connection.getMetaData().getDriverName();
        } catch (Exception e) {
            throw new OssException("数据源连接失败，请检查配置!");
        }
        if (driverName.contains("Oracle")) {
            jdbcOssOperation = new OracleOssOperation();
        } else if (driverName.contains("MySQL")) {
            jdbcOssOperation = new MySQLOssOperation();
        } else {
            throw new NotSupportException("不支持的数据库!");
        }
        jdbcOssOperation.setJdbcTemplate(new JdbcTemplate(dataSource));
    }
}
