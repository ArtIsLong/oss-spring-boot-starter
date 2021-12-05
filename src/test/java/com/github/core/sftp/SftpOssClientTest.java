package com.github.core.sftp;

import cn.hutool.core.io.FileUtil;
import com.github.core.StandardOssClient;
import com.github.core.model.OssInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 陈敏
 * @version SftpOssClientTest.java, v 1.1 2021/11/23 4:53 chenmin Exp $
 * Created on 2021/11/23
 */
@SpringBootTest
public class SftpOssClientTest {

    @Autowired
    private StandardOssClient ossClient;

    @Test
    void upLoad() {
        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("C:\\Users\\15221\\Desktop\\vim.png"), "/test/test1/vim.png");
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
        OssInfo ossInfo = ossClient.getInfo("/test", true);
        System.out.println(ossInfo);
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