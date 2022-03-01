package io.github.artislong.core;

import cn.hutool.core.io.FileUtil;
import io.github.artislong.model.OssInfo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author 陈敏
 * @version StandardOssClientTest.java, v 1.1 2022/3/1 11:32 chenmin Exp $
 * Created on 2022/3/1
 */
public interface StandardOssClientTest {

    default void upLoad() {
        OssInfo ossInfo = getOssClient().upLoad(FileUtil.getInputStream("/Users/admin/test.png"), "test.png");
        System.out.println(ossInfo);
    }

    default void upLoadCheckPoint() {
        OssInfo ossInfo = getOssClient().upLoadCheckPoint("/Users/admin/test.zip", "test.zip");
        System.out.println(ossInfo);
    }

    default void downLoad() throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream("/Users/admin/test.png");
        getOssClient().downLoad(fileOutputStream, "test1.png");
    }

    default void downloadCheckPoint() {
        getOssClient().downLoadCheckPoint("/Users/admin/test.zip", "test.zip");
    }

    default void delete() {
        getOssClient().delete("test1.png");
    }

    default void copy() {
        getOssClient().copy("test.png", "test1.png");
    }

    default void move() {
        getOssClient().move("test1.png", "test2.png");
    }

    default void rename() {
        getOssClient().rename("test2.png", "test1.png");
    }

    default void getInfo() {
        OssInfo info = getOssClient().getInfo("test.png");
        System.out.println(info);
        info = getOssClient().getInfo("/", true);
        System.out.println(info);
    }

    default void isExist() {
        System.out.println(getOssClient().isExist("test.png"));
    }

    StandardOssClient getOssClient();

}
