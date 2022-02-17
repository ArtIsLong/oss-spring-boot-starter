package io.github.artislong.core.baidu;

import cn.hutool.core.io.FileUtil;
import com.baidubce.services.bos.BosClient;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.model.OssInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author 陈敏
 * @version BaiduOssClientTest.java, v 1.1 2021/11/25 10:11 chenmin Exp $
 * Created on 2021/11/25
 */
@SpringBootTest
public class BaiduOssClientTest {

    @Autowired
    private StandardOssClient ossClient;
    @Autowired
    private BosClient bosClient;

    @Test
    void upLoad() {
        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("C:\\Users\\15221\\Desktop\\vim.png"), "vim1.png");
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
        ossClient.rename("vim2.png", "vim3.png");
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

    @Test
    void isFile() {
    }

    @Test
    void isDirectory() {
    }

}