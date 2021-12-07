package io.github.artislong.core.up;

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
 * @version UpOssClientTest.java, v 1.1 2021/12/3 20:31 chenmin Exp $
 * Created on 2021/12/3
 */
@SpringBootTest
public class UpOssClientTest {

    @Autowired
    private StandardOssClient upOssClient;

    @Test
    void upLoad() {
        OssInfo ossInfo = upOssClient.upLoad(FileUtil.getInputStream("C:\\Users\\15221\\Desktop\\vim.png"), "vim.png");
        System.out.println(ossInfo);
    }

    @Test
    void downLoad() throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream("C:\\Users\\15221\\Desktop\\vim1.png");
        upOssClient.downLoad(fileOutputStream, "/vim.png");
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
//        System.out.println(upOssClient.getInfo("/vim.png"));
        OssInfo ossInfo = upOssClient.getInfo("/", true);
        System.out.println(ossInfo);
    }

    @Test
    void isExist() {
    }

    @Test
    void createFile() {
    }

    @Test
    void createDirectory() {
    }
}