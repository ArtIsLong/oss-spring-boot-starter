package com.github.core.minio;

import com.github.OssProperties;
import com.github.core.StandardOssClient;
import com.github.core.model.OssInfo;
import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author 陈敏
 * @version MinioOssClient.java, v 1.1 2021/11/24 15:35 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinioOssClient implements StandardOssClient {

    private MinioClient minioClient;
    private OssProperties ossProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        return null;
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
    }

    @Override
    public void delete(String targetName) {

    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {

    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {

    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {

    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        return null;
    }

    @Override
    public Boolean isExist(String targetName) {
        return null;
    }

    @Override
    public Boolean isFile(String targetName) {
        return null;
    }

    @Override
    public Boolean isDirectory(String targetName) {
        return null;
    }

    @Override
    public OssInfo createFile(String targetName) {
        return null;
    }

    @Override
    public OssInfo createDirectory(String targetName) {
        return null;
    }

    private String getBucket() {
        return null;
    }
}
