package io.github.artislong.core.fdfs;

import com.github.tobato.fastdfs.domain.upload.FastFile;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.fdfs.model.FdfsOssConfig;
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
 * @author 陈敏
 * @version FdfsOssClient.java, v 1.0 2022/10/12 21:45 chenmin Exp $
 * Created on 2022/10/12
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FdfsOssClient implements StandardOssClient {

    private FastFileStorageClient fastFileStorageClient;
    private FdfsOssConfig fdfsOssConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        FastFile.Builder builder = new FastFile.Builder();
        FastFile fastFile = builder.build();
        fastFileStorageClient.uploadFile(fastFile);
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
