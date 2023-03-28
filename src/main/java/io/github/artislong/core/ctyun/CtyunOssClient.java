package io.github.artislong.core.ctyun;

import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ctyun.model.CtyunOssConfig;
import io.github.artislong.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * https://www.ctyun.cn/document/10000101
 * @author 陈敏
 * @version CtyunOssClient.java, v 1.0 2022/5/11 18:37 chenmin Exp $
 * Created on 2022/5/11
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CtyunOssClient implements StandardOssClient {

    private CtyunOssConfig ctyunOssConfig;

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
        return null;
    }

    @Override
    public String getBasePath() {
        return null;
    }
}
