package io.github.artislong.core.inspur;

import com.inspurcloud.oss.client.OSSClient;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.inspur.model.InspurOssConfig;
import io.github.artislong.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * https://console1.cloud.inspur.com/document/oss/index.html
 * @author 陈敏
 * @version InspurOssClient.java, v 1.0 2022/5/17 0:48 chenmin Exp $
 * Created on 2022/5/17
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InspurOssClient implements StandardOssClient {

    public static final String OSS_OBJECT_NAME = "ossClient";

    private OSSClient ossClient;
    private InspurOssConfig inspurOssConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        return null;
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        return null;
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {

    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {

    }

    @Override
    public void delete(String targetName) {

    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {

    }

    @Override
    public OssInfo getInfo(String targetName, boolean isRecursion) {
        return null;
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(OSS_OBJECT_NAME, getOssClient());
            }
        };
    }

    @Override
    public String getBasePath() {
        return null;
    }

    public String getBucketName() {
        String bucketName = inspurOssConfig.getBucketName();
        if (ossClient.doesBucketExist(bucketName)) {
            ossClient.createBucket(bucketName);
        }
        return bucketName;
    }

}
