package io.github.artislong.core.ali;

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
 * @version AliOssClientTest.java, v 1.1 2021/11/16 21:19 chenmin Exp $
 * Created on 2021/11/16
 */
@SpringBootTest
class AliOssClientTest {

    @Autowired
    private StandardOssClient ossClient;

    @Test
    void upLoad() {
        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("/Users/vim.png"), "vim.png");
        System.out.println(ossInfo);
    }

    @Test
    void upLoadCheckPoint() {
        OssInfo ossInfo = ossClient.upLoadCheckPoint("F:\\影片\\饥饿站台BD中字.mp4", "饥饿站台BD中字.mp4");
        System.out.println(ossInfo);
    }

    @Test
    void downLoad() throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream("C:\\Users\\15221\\Desktop\\vim1.png");
        ossClient.downLoad(fileOutputStream, "vim.png");
    }

    @Test
    void delete() {
        ossClient.delete("vim1.png");
    }

    @Test
    void copy() {
        ossClient.copy("vim.png", "vim1.png");
    }

    @Test
    void move() {
        ossClient.move("vim1.png", "vim2.png");
    }

    @Test
    void rename() {
        ossClient.rename("vim2.png", "vim1.png");
    }

    @Test
    void getInfo() {
//        OssInfo info = ossClient.getInfo("vim3.png");
        OssInfo info = ossClient.getInfo("/", true);
        System.out.println(info);
    }

    @Test
    void isExist() {
        System.out.println(ossClient.isExist("vim.png"));
    }
}