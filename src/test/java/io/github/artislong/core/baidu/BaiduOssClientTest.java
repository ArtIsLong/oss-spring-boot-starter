package io.github.artislong.core.baidu;

import cn.hutool.core.date.StopWatch;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.OssInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 陈敏
 * @version BaiduOssClientTest.java, v 1.1 2021/11/25 10:11 chenmin Exp $
 * Created on 2021/11/25
 */
@SpringBootTest
public class BaiduOssClientTest {

    @Autowired
    private StandardOssClient ossClient;

    @Test
    void upLoad() {
    }

    @Test
    void upLoadCheckPoint() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        OssInfo ossInfo = ossClient.upLoadCheckPoint("/Users/chenmin/study/data/data.zip", "data.zip");
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
        System.out.println(ossInfo);
    }

    @Test
    void downLoad() {
    }

    @Test
    void delete() {
    }

    @Test
    void copy() {
    }

    @Test
    void move() {
    }

    @Test
    void rename() {
    }

    @Test
    void getInfo() {
    }

    @Test
    void isExist() {
    }

    @Test
    void isFile() {
    }

    @Test
    void isDirectory() {
    }

    @Test
    void createFile() {
    }

    @Test
    void createDirectory() {
    }
}