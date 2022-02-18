package io.github.artislong.core.minio;

import cn.hutool.core.io.FileUtil;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.model.OssInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author 陈敏
 * @version MinioOssClientTest.java, v 1.1 2021/11/25 10:15 chenmin Exp $
 * Created on 2021/11/25
 */
@SpringBootTest
public class MinioOssClientTest {

    @Autowired
    private StandardOssClient ossClient;

    @Test
    void upLoad() {
        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("/Users/admin/test.png"), "test.png");
        System.out.println(ossInfo);
    }

    @Test
    void upLoadCheckPoint() {
        OssInfo ossInfo = ossClient.upLoadCheckPoint("/Users/admin/test.data", "/Users/admin/test.data");
        System.out.println(ossInfo);
    }

    @Test
    void downLoad() throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream("/Users/admin/test.png");
        ossClient.downLoad(fileOutputStream, "test1.png");
    }

    @Test
    void delete() {
        ossClient.delete("test1.png");
    }

    @Test
    void copy() {
        ossClient.copy("test.png", "test1.png");
    }

    @Test
    void move() {
        ossClient.move("test1.png", "test2.png");
    }

    @Test
    void rename() {
        ossClient.rename("test2.png", "test1.png");
    }

    @Test
    void getInfo() {
        OssInfo info = ossClient.getInfo("test.png");
        System.out.println(info);
        info = ossClient.getInfo("/", true);
        System.out.println(info);
    }

    @Test
    void isExist() {
        System.out.println(ossClient.isExist("test.png"));
    }
}