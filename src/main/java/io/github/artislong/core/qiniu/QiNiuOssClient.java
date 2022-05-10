package io.github.artislong.core.qiniu;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
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
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.qiniu.model.QiNiuOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.utils.OssPathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static final String AUTH_OBJECT_NAME = "auth";
    public static final String UPLOAD_OBJECT_NAME = "uploadManager";
    public static final String BUCKET_OBJECT_NAME = "bucketManager";

    private Auth auth;
    private UploadManager uploadManager;
    private BucketManager bucketManager;
    private QiNiuOssConfig qiNiuOssConfig;
    private Configuration configuration;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        try {
            uploadManager.put(is, getKey(targetName, false), getUpToken(), null, null);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s上传失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
        return getInfo(targetName, false);
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        String key = getKey(targetName, false);

        try {
            FileRecorder fileRecorder = new FileRecorder(file.getParent());
            UploadManager uploadManager = new UploadManager(configuration, fileRecorder);

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
        DownloadUrl downloadUrl = new DownloadUrl("qiniu.com", false, getKey(targetName, false));
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
    public void downLoadCheckPoint(File localFile, String targetName) {
        downLoadFile(localFile, targetName, qiNiuOssConfig.getSliceConfig(), OssConstant.OssType.QINIU);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        try {
            FileInfo fileInfo = bucketManager.stat(getBucket(), getKey(targetName, false));
            return new DownloadObjectStat().setSize(fileInfo.fsize)
                    .setLastModified(DateUtil.date(fileInfo.putTime / 10000))
                    .setDigest(fileInfo.md5);
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public void prepareDownload(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) {
        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);
        downloadCheckPoint.setDownloadFile(localFile.getPath());
        downloadCheckPoint.setBucketName(getBucket());
        downloadCheckPoint.setKey(getKey(targetName, false));
        downloadCheckPoint.setCheckPointFile(checkpointFile);

        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));

        long downloadSize;
        if (downloadCheckPoint.getObjectStat().getSize() > 0) {
            Long partSize = qiNiuOssConfig.getSliceConfig().getPartSize();
            long[] slice = getDownloadSlice(new long[0], downloadCheckPoint.getObjectStat().getSize());
            downloadCheckPoint.setDownloadParts(splitDownloadFile(slice[0], slice[1], partSize));
            downloadSize = slice[1];
        } else {
            //download whole file
            downloadSize = 0;
            downloadCheckPoint.setDownloadParts(splitDownloadOneFile());
        }
        downloadCheckPoint.setOriginPartSize(downloadCheckPoint.getDownloadParts().size());
        createDownloadTemp(downloadCheckPoint.getTempDownloadFile(), downloadSize);
    }

    @Override
    public InputStream downloadPart(String key, long start, long end) {
        try {
            DownloadUrl downloadUrl = new DownloadUrl("qiniu.com", false, key);
            String url = downloadUrl.buildURL();
            HttpResponse response = HttpUtil.createGet(url, true)
                    .timeout(-1)
                    .header("Range", "bytes=" + start + "-" + end)
                    .execute();
            log.debug("start={}, end={}", start, end);
            return new ByteArrayInputStream(response.bodyBytes());
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public void delete(String targetName) {
        try {
            bucketManager.delete(getBucket(), getKey(targetName, false));
        } catch (QiniuException e) {
            String errorMsg = String.format("%s删除失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.copy(getBucket(), getKey(sourceName, false), getBucket(), getKey(targetName, false), isOverride);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s复制失败", targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.move(getBucket(), getKey(sourceName, false), getBucket(), getKey(targetName, false), isOverride);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s移动到%s失败", sourceName, targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {
        try {
            bucketManager.rename(getBucket(), getKey(sourceName, false), getKey(targetName, false), isOverride);
        } catch (QiniuException e) {
            String errorMsg = String.format("%s重命名为%s失败", sourceName, targetName);
            log.error(errorMsg, e);
            throw new OssException(errorMsg, e);
        }
    }

    @SneakyThrows
    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(targetName);
        if (isRecursion && isDirectory(key)) {
            FileListing listFiles = bucketManager.listFiles(getBucket(), key, "", 1000, StrUtil.SLASH);

            System.out.println(listFiles);
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(listFiles.items)) {
                for (FileInfo fileInfo : listFiles.items) {
                    fileOssInfos.add(getInfo(OssPathUtil.replaceKey(fileInfo.key, getBasePath(), false), false));
                }
            }

            if (ObjectUtil.isNotEmpty(listFiles.commonPrefixes)) {
                for (String commonPrefix : listFiles.commonPrefixes) {
                    String target = OssPathUtil.replaceKey(commonPrefix, getBasePath(), true);
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

    @Override
    public String getBasePath() {
        return qiNiuOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(AUTH_OBJECT_NAME, getAuth());
                put(UPLOAD_OBJECT_NAME, getUploadManager());
                put(BUCKET_OBJECT_NAME, getBucketManager());
            }
        };
    }

    private String getUpToken() {
        return auth.uploadToken(getBucket());
    }

    private String getBucket() {
        String bucketName = qiNiuOssConfig.getBucketName();
        try {
            if (ObjectUtil.isEmpty(bucketManager.getBucketInfo(bucketName))) {
                bucketManager.createBucket(bucketName, qiNiuOssConfig.getClientConfig().getRegion().getRegion());
            }
        } catch (QiniuException e) {
            log.error("创建Bucket失败", e);
        }
        return bucketName;
    }

    private OssInfo getBaseInfo(String targetName) {
        String key = getKey(targetName, false);
        OssInfo ossInfo;
        if (isFile(targetName)) {
            ossInfo = new FileOssInfo();
            try {
                FileInfo fileInfo = bucketManager.stat(getBucket(), key);
                String putTime = DateUtil.date(fileInfo.putTime / 10000).toString(DatePattern.NORM_DATETIME_PATTERN);
                ossInfo.setLength(Convert.toStr(fileInfo.fsize));
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
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));

        return ossInfo;
    }
}
