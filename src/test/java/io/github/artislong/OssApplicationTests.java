package io.github.artislong;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author 陈敏
 * @version OssApplicationTests.java, v 1.1 2021/11/5 14:52 chenmin Exp $
 * Created on 2021/11/5
 */
//@SpringBootTest
public class OssApplicationTests {

    @Test
    public void context() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
//        dataSource.setJdbcUrl("jdbc:oracle:thin:@39.105.163.75:1521:orcl");
//        dataSource.setUsername("test");
//        dataSource.setPassword("test123");

        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/oss?characterEncoding=UTF-8&useUnicode=true&useSSL=false&tinyInt1isBit=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        Connection connection = dataSource.getConnection();
        System.out.println(connection.getMetaData().getDriverName());
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDataSource(dataSource);
        System.out.println(hikariDataSource.getDriverClassName());
    }

}
