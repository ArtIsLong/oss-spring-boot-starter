package io.github.artislong.core.qiniu;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.storage.persistent.FileRecorder;
import com.qiniu.util.Auth;
import io.github.artislong.OssProperties;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * https://developer.qiniu.com/kodo
 *
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
            uploadManager.put(is, getKey(targetName, true), getUpToken(), null, null);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s上传失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
        return getInfo(targetName, false);
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        String key = getKey(targetName, true);
        String parentPath = convertPath(Paths.get(key).getParent().toString(), true);

        QiNiuOssProperties qiNiuOssProperties = getQiNiuOssProperties();
        SliceConfig sliceConfig = qiNiuOssProperties.getSliceConfig();

        Configuration cfg = new Configuration(qiNiuOssProperties.getRegion().buildRegion());
        // 指定分片上传版本
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        // 设置分片上传并发，1：采用同步上传；大于1：采用并发上传
        cfg.resumableUploadMaxConcurrentTaskCount = sliceConfig.getTaskNum();
        cfg.resumableUploadAPIV2BlockSize = sliceConfig.getPartSize().intValue();

        try {
            FileRecorder fileRecorder = new FileRecorder(parentPath);
            UploadManager uploadManager = new UploadManager(cfg, fileRecorder);
            uploadManager.put(file.getPath(), key, getUpToken());
        } catch (Exception e) {
            String errorMsg = String.format("%s上传失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        DownloadUrl downloadUrl = new DownloadUrl("", false, getKey(targetName, true));
        try {
            String url = downloadUrl.buildURL();
            HttpUtil.download(url, os, false);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s下载失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @Override
    public void delete(String targetName) {
        try {
            bucketManager.delete(getBucket(), getKey(targetName, true));
        } catch (QiniuException e) {
            String errorMsg = String.format("%s删除失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.copy(getBucket(), getKey(sourceName, true), getBucket(), getKey(targetName, true), isOverride);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s复制失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.move(getBucket(), getKey(sourceName, true), getBucket(), getKey(targetName, false), isOverride);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s移动到%s失败", sourceName, targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.rename(getBucket(), getKey(sourceName, true), getKey(targetName, true), isOverride);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s重命名为%s失败", sourceName, targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @SneakyThrows
    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, true);

        OssInfo ossInfo = getBaseInfo(targetName);
        if (isRecursion && isDirectory(key)) {
            FileListing listFiles = bucketManager.listFiles(getBucket(), key, "", 1000, "/");

            System.out.println(listFiles);
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(listFiles.items)) {
                for (FileInfo fileInfo : listFiles.items) {
                    fileOssInfos.add(getInfo(replaceKey(fileInfo.key, getBasePath(), true), false));
                }
            }

            if (ObjectUtil.isNotEmpty(listFiles.commonPrefixes)) {
                for (String commonPrefix : listFiles.commonPrefixes) {
                    String target = replaceKey(commonPrefix, getBasePath(), true);
                    directoryInfos.add(getInfo(target, true));
                }
            }
            if (ObjectUtil.isNotEmpty(fileOssInfos) && fileOssInfos.get(0) instanceof FileOssInfo) {
                ReflectUtil.setFieldValue(ossInfo, "fileInfos", fileOssInfos);
            }
            if (ObjectUtil.isNotEmpty(directoryInfos) && directoryInfos.get(0) instanceof DirectoryOssInfo) {
                ReflectUtil.setFieldValue(ossInfo, "directoryInfos", directoryInfos);
            }
        }
        return ossInfo;
    }

    private String getUpToken() {
        return auth.uploadToken(getBucket());
    }

    private String getBucket() {
        return qiNiuOssProperties.getBucketName();
    }

    private OssInfo getBaseInfo(String targetName) {
        String key = getKey(targetName, true);
        OssInfo ossInfo;
        if (isFile(targetName)) {
            ossInfo = new FileOssInfo();
            try {
                FileInfo fileInfo = bucketManager.stat(getBucket(), key);
                String putTime = DateUtil.date(fileInfo.putTime / 10000).toString(DatePattern.NORM_DATETIME_PATTERN);
                ossInfo.setSize(Convert.toStr(fileInfo.fsize));
                ossInfo.setCreateTime(putTime);
                ossInfo.setLastUpdateTime(putTime);
            } catch (QiniuException e) {
                String errorMsg = String.format("获取%s信息失败", targetName);
                log.error(errorMsg, e);
                throw new OssException(errorMsg, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }

        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));

        return ossInfo;
    }
}
