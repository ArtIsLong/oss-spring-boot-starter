package io.github.artislong.core.jdbc;

import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.StandardOssClientTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 陈敏
 * @version JdbcOssClientTest.java, v 1.0 2022/3/14 0:04 chenmin Exp $
 * Created on 2022/3/14
 */
@SpringBootTest
public class JdbcOssClientTest implements StandardOssClientTest {

    @Getter
    @Autowired
    @Qualifier(JdbcOssConfiguration.DEFAULT_BEAN_NAME)
    private StandardOssClient ossClient;

    @Test
    public void test() throws Exception {
//        upLoad();
//        downLoad();
//        copy();
//        rename();
//        move();
//        isExist();
        getInfo();
//        delete();
//
//        upLoadCheckPoint();
//        downloadCheckPoint();
    }

}