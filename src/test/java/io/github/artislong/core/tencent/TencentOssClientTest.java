package io.github.artislong.core.tencent;

import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.StandardOssClientTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 陈敏
 * @version TencentOssClientTest.java, v 1.1 2021/11/25 10:18 chenmin Exp $
 * Created on 2021/11/25
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class TencentOssClientTest implements StandardOssClientTest {

    @Getter
    @Autowired
    @Qualifier(TencentOssConfiguration.DEFAULT_BEAN_NAME)
    private StandardOssClient ossClient;

    @Test
    public void test() throws Exception {
        upLoad();
        downLoad();
        copy();
        rename();
        move();
        isExist();
        getInfo();
        delete();

        upLoadCheckPoint();
        downloadCheckPoint();
    }

}