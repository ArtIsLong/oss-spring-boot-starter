package com.github.core.qiniu;

import cn.hutool.http.HttpUtil;
import com.github.OssProperties;
import com.github.core.StandardOssClient;
import com.github.core.model.OssInfo;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author 陈敏
 * @version QiNiuOssClient.java, v 1.1 2021/11/15 11:13 chenmin Exp $
 * Created on 2021/11/15
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QiNiuOssClient implements StandardOssClient {

    private Auth auth;
    private UploadManager uploadManager;
    private BucketManager bucketManager;
    private OssProperties ossProperties;
    private QiNiuOssProperties qiNiuOssProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        try {
            Response response = uploadManager.put(is, targetName, getUpToken(), null, null);
        } catch (QiniuException e) {
            log.error("{}上传失败", targetName, e);
        }
        return null;
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        DownloadUrl downloadUrl = new DownloadUrl("", false, targetName);
        try {
            String url = downloadUrl.buildURL();
            HttpUtil.download(url, os, false);
        } catch (QiniuException e) {
            log.error("", e);
        }
    }

    @Override
    public void delete(String targetName) {
        try {
            bucketManager.delete(getBucket(), targetName);
        } catch (QiniuException e) {
            log.error("", e);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.copy(getBucket(), sourceName, getBucket(), targetName, isOverride);
        } catch (QiniuException e) {
            log.error("", e);
        }
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.move(getBucket(), sourceName, getBucket(), targetName, isOverride);
        } catch (QiniuException e) {
            log.error("", e);
        }
    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.rename(getBucket(), sourceName, targetName, isOverride);
        } catch (QiniuException e) {
            log.error("", e);
        }
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

    private String getUpToken() {
        return null;
    }

    private String getBucket() {
        return null;
    }
}
