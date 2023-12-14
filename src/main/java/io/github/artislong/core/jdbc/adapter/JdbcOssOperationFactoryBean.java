package io.github.artislong.core.jdbc.adapter;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.core.jdbc.constant.DbType;
import io.github.artislong.exception.NotSupportException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 陈敏
 * @version JdbcOssOperationFactory.java, v 1.0 2022/5/25 17:56 chenmin Exp $
 * Created on 2022/5/25
 */
public class JdbcOssOperationFactoryBean implements FactoryBean<JdbcOssOperation>, InitializingBean {

    public static final Map<DbType, JdbcOssOperation> JDBC_OSS_OPERATION_MAP = new HashMap<>();

    static {
        Set<Class<?>> jdbcOssOperationClasses = ClassUtil.scanPackageBySuper(StrUtil.EMPTY, JdbcOssOperation.class);
        for (Class<?> jdbcOssOperationClass : jdbcOssOperationClasses) {
            JdbcOssOperation jdbcOssOperation = (JdbcOssOperation) ReflectUtil.newInstance(jdbcOssOperationClass);
            JDBC_OSS_OPERATION_MAP.put(jdbcOssOperation.getDbType(), jdbcOssOperation);
        }
    }

    @Setter
    @Getter
    private DataSource dataSource;
    private JdbcOssOperation jdbcOssOperation;

    @Override
    public JdbcOssOperation getObject() {
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
        DbType dbType = DbType.getDbType(dataSource);
        if (!JDBC_OSS_OPERATION_MAP.containsKey(dbType)) {
            throw new NotSupportException(String.format("%s数据库暂未实现", dbType.getDb()));
        }
        this.jdbcOssOperation = JDBC_OSS_OPERATION_MAP.get(dbType);
        this.jdbcOssOperation.setJdbcTemplate(new JdbcTemplate(dataSource));
    }
}
