package io.github.artislong.core.ali;

import cn.hutool.core.io.FileUtil;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.OssInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author 陈敏
 * @version AliOssClientTest.java, v 1.1 2021/11/16 21:19 chenmin Exp $
 * Created on 2021/11/16
 */
@SpringBootTest
class AliOssClientTest {

    @Autowired
    private StandardOssClient ossClient;

    @Test
    void upLoad() {
//        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("C:\\Users\\15221\\Desktop\\vim.png"), "/test/test1/vim.png");
//        System.out.println(ossInfo);
        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("C:\\Users\\15221\\Desktop\\vim.png"), "vim1.png");
        System.out.println(ossInfo);
    }

    @Test
    void downLoad() throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream("C:\\Users\\15221\\Desktop\\vim1.png");
        ossClient.downLoad(fileOutputStream, "vim1.png");
    }

    @Test
    void delete() {
        ossClient.delete("/test/test1/vim1.png");
    }

    @Test
    void copy() {
        ossClient.copy("/test/test1/vim.png", "/test/test1/vim2.png");
    }

    @Test
    void move() {
        ossClient.move("/test/test1/vim2.png", "/test/test1/vim3.png");
    }

    @Test
    void rename() {
        ossClient.rename("/test/test1/vim3.png", "/test/test1/vim2.png");
    }

    @Test
    void getInfo() {
        OssInfo ossInfo = ossClient.getInfo("/Study/", true);
        System.out.println(ossInfo);
    }

    @Test
    void isExist() {
        System.out.println(ossClient.isExist("/test/test1"));
    }

    @Test
    void isFile() {
        System.out.println(ossClient.isFile("/test/test1/vim0.png"));
    }

    @Test
    void isDirectory() {
        System.out.println(ossClient.isDirectory("/test/test1g"));
    }

    @Test
    void createFile() {
        System.out.println(ossClient.createFile("/test/test.txt"));
    }

    @Test
    void createDirectory() {
        System.out.println(ossClient.createDirectory("/test001"));
    }
}