package io.github.artislong.core.local;

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
 * @version LocalOssClientTest.java, v 1.1 2021/11/15 15:23 chenmin Exp $
 * Created on 2021/11/15
 */
@SpringBootTest
public class LocalOssClientTest {

    @Autowired
    private StandardOssClient ossClient;

    @Test
    void upLoad() {
        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("C:\\Users\\15221\\Desktop\\vim.png"), "vim.png");
        System.out.println(ossInfo);
    }

    @Test
    void downLoad() throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream("C:\\Users\\15221\\Desktop\\vim1.png");
        ossClient.downLoad(fileOutputStream, "/vim.png");
    }

    @Test
    void delete() {
        ossClient.delete("/vim.png");
    }

    @Test
    void copy() {
        ossClient.copy("/vim.png", "/vim1.png");
    }

    @Test
    void move() {
        ossClient.move("/test/vim.png", "");
    }

    @Test
    void rename() {
        ossClient.rename("/test/vim.png", "/test/vim1.png");
    }

    @Test
    void getInfo() {
//        System.out.println(ossClient.getInfo("/vim.png"));
        OssInfo ossInfo = ossClient.getInfo("/test", true);
        System.out.println(ossInfo);
    }

    @Test
    void isExist() {
        System.out.println(ossClient.isExist("/test/vim1.png"));
    }

    @Test
    void isFile() {
        System.out.println(ossClient.isFile("/test/vim1.png"));
    }

    @Test
    void isDirectory() {
        System.out.println(ossClient.isDirectory("/test/vim1.png"));
    }

    @Test
    void create() {
        System.out.println(ossClient.createDirectory("/test2/"));
        System.out.println(ossClient.createFile("/test4/test.txt"));
    }
}