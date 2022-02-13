package io.github.artislong.core.baidu;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.AbortMultipartUploadRequest;
import com.baidubce.services.bos.model.MultipartUploadSummary;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.model.OssInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.List;

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
    public void test01() {
        bosClient.putSuperObjectFromFile(new File("/Users/chenmin/study/data/data.zip"), "artislong", "Study/data.zip");
    }

    @Test
    void upLoad() {
        OssInfo ossInfo = ossClient.upLoad(FileUtil.getInputStream("/Users/chenmin/Desktop/mac按键.png"), "mac按键.png");
        System.out.println(ossInfo);
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