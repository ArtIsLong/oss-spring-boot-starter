package io.github.artislong.core.qiniu;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
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
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.download.DownloadPart;
import io.github.artislong.model.download.DownloadPartResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
    private QiNiuOssConfig qiNiuOssConfig;

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

        SliceConfig sliceConfig = qiNiuOssConfig.getSliceConfig();

        Configuration cfg = new Configuration(qiNiuOssConfig.getRegion().buildRegion());
        // 指定分片上传版本
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        // 设置分片上传并发，1：采用同步上传；大于1：采用并发上传
        cfg.resumableUploadMaxConcurrentTaskCount = sliceConfig.getTaskNum();
        cfg.resumableUploadAPIV2BlockSize = sliceConfig.getPartSize().intValue();

        try {
            FileRecorder fileRecorder = new FileRecorder(file.getParent());
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

        String checkpointFile = localFile.getPath() + StrUtil.DOT + OssConstant.OssType.QINIU;

        DownloadCheckPoint downloadCheckPoint = new DownloadCheckPoint();
        try {
            downloadCheckPoint.load(checkpointFile);
        } catch (Exception e) {
            FileUtil.del(checkpointFile);
        }

        try {
            DownloadObjectStat downloadObjectStat = getDownloadObjectStat(targetName);
            if (!downloadCheckPoint.isValid(downloadObjectStat)) {
                prepare(downloadCheckPoint, localFile, targetName, checkpointFile);
                FileUtil.del(checkpointFile);
            }
        } catch (Exception e) {
            throw new OssException(e);
        }

        SliceConfig slice = qiNiuOssConfig.getSliceConfig();

        ExecutorService executorService = Executors.newFixedThreadPool(slice.getTaskNum());
        List<Future<DownloadPartResult>> futures = new ArrayList<>();

        for (int i = 0; i < downloadCheckPoint.getDownloadParts().size(); i++) {
            if (!downloadCheckPoint.getDownloadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new DownloadPartTask(downloadCheckPoint, i)));
            }
        }

        executorService.shutdown();

        for (Future<DownloadPartResult> future : futures) {
            try {
                DownloadPartResult partResult = future.get();
                if (partResult.isFailed()) {
                    throw partResult.getException();
                }
            } catch (Exception e) {
                throw new OssException(e);
            }
        }

        try {
            if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new OssException("关闭线程池失败", e);
        }

        FileUtil.rename(new File(downloadCheckPoint.getTempDownloadFile()), downloadCheckPoint.getDownloadFile(), true);
        FileUtil.del(downloadCheckPoint.getCheckPointFile());
    }

    private DownloadObjectStat getDownloadObjectStat(String targetName) throws QiniuException {
        FileInfo fileInfo = bucketManager.stat(getBucket(), getKey(targetName, false));
        return new DownloadObjectStat().setSize(fileInfo.fsize)
                .setLastModified(DateUtil.date(fileInfo.putTime / 10000))
                .setDigest(fileInfo.md5);
    }

    private void prepare(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) throws QiniuException {
        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);
        downloadCheckPoint.setDownloadFile(localFile.getPath());
        downloadCheckPoint.setBucketName(getBucket());
        downloadCheckPoint.setKey(getKey(targetName, false));
        downloadCheckPoint.setCheckPointFile(checkpointFile);

        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));

        long downloadSize;
        if (downloadCheckPoint.getObjectStat().getSize() > 0) {
            Long partSize = qiNiuOssConfig.getSliceConfig().getPartSize();
            long[] slice = getSlice(new long[0], downloadCheckPoint.getObjectStat().getSize());
            downloadCheckPoint.setDownloadParts(splitDownloadFile(slice[0], slice[1], partSize));
            downloadSize = slice[1];
        } else {
            //download whole file
            downloadSize = 0;
            downloadCheckPoint.setDownloadParts(splitDownloadOneFile());
        }
        downloadCheckPoint.setOriginPartSize(downloadCheckPoint.getDownloadParts().size());
        downloadCheckPoint.setVersionId(IdUtil.fastSimpleUUID());
        createFixedFile(downloadCheckPoint.getTempDownloadFile(), downloadSize);
    }

    private ArrayList<DownloadPart> splitDownloadFile(long start, long objectSize, long partSize) {
        ArrayList<DownloadPart> parts = new ArrayList<>();

        long partNum = objectSize / partSize;
        if (partNum >= OssConstant.DEFAULT_PART_NUM) {
            partSize = objectSize / (OssConstant.DEFAULT_PART_NUM - 1);
        }

        long offset = 0L;
        for (int i = 0; offset < objectSize; offset += partSize, i++) {
            DownloadPart part = new DownloadPart();
            part.setIndex(i);
            part.setStart(offset + start);
            part.setEnd(getPartEnd(offset, objectSize, partSize) + start);
            part.setFileStart(offset);
            parts.add(part);
        }

        return parts;
    }

    private long getPartEnd(long begin, long total, long per) {
        if (begin + per > total) {
            return total - 1;
        }
        return begin + per - 1;
    }

    private ArrayList<DownloadPart> splitDownloadOneFile() {
        ArrayList<DownloadPart> parts = new ArrayList<>();
        DownloadPart part = new DownloadPart();
        part.setIndex(0);
        part.setStart(0);
        part.setEnd(-1);
        part.setFileStart(0);
        parts.add(part);
        return parts;
    }

    private long[] getSlice(long[] range, long totalSize) {
        long start = 0;
        long size = totalSize;

        if ((range == null) ||
                (range.length != 2) ||
                (totalSize < 1) ||
                (range[0] < 0 && range[1] < 0) ||
                (range[0] > 0 && range[1] > 0 && range[0] > range[1]) ||
                (range[0] >= totalSize)) {
            //download all
        } else {
            //dwonload part by range & total size
            long begin = range[0];
            long end = range[1];
            if (range[0] < 0) {
                begin = 0;
            }
            if (range[1] < 0 || range[1] >= totalSize) {
                end = totalSize - 1;
            }
            start = begin;
            size = end - begin + 1;
        }

        return new long[]{start, size};
    }

    public static void createFixedFile(String filePath, long length) {
        File file = new File(filePath);
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(file, "rw");
            rf.setLength(length);
        } catch (Exception e) {
            throw new OssException("创建下载缓存文件失败");
        } finally {
            IoUtil.close(rf);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DownloadPartTask implements Callable<DownloadPartResult> {

        DownloadCheckPoint downloadCheckPoint;
        int partNum;

        @Override
        public DownloadPartResult call() {
            DownloadPartResult partResult = null;
            RandomAccessFile output = null;
            InputStream content = null;
            try {
                DownloadPart downloadPart = downloadCheckPoint.getDownloadParts().get(partNum);

                partResult = new DownloadPartResult(partNum + 1, downloadPart.getStart(), downloadPart.getEnd());

                output = new RandomAccessFile(downloadCheckPoint.getTempDownloadFile(), "rw");
                output.seek(downloadPart.getFileStart());

                HttpResponse response = downLoadPart(downloadCheckPoint.getKey(), downloadPart.getStart(), downloadPart.getEnd());
                output.write(response.bodyBytes(), Convert.toInt(downloadPart.getStart()), -1);
                partResult.setLength(downloadPart.getLength());
                downloadCheckPoint.update(partNum, true);
                downloadCheckPoint.dump();
            } catch (Exception e) {
                partResult.setException(e);
                partResult.setFailed(true);
            } finally {
                IoUtil.close(output);
                IoUtil.close(content);
            }
            return partResult;
        }
    }

    private static HttpResponse downLoadPart(String key, long start, long end) throws QiniuException {
        DownloadUrl downloadUrl = new DownloadUrl("qiniu.com", false, key);
        String url = downloadUrl.buildURL();
        return HttpUtil.createGet(url, true)
                .timeout(-1)
                .header("Range", "bytes=" + start + "-" + end)
                .execute();
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
            FileListing listFiles = bucketManager.listFiles(getBucket(), key, "", 1000, "/");

            System.out.println(listFiles);
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(listFiles.items)) {
                for (FileInfo fileInfo : listFiles.items) {
                    fileOssInfos.add(getInfo(replaceKey(fileInfo.key, getBasePath(), false), false));
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

    @Override
    public String getBasePath() {
        return qiNiuOssConfig.getBasePath();
    }

    private String getUpToken() {
        return auth.uploadToken(getBucket());
    }

    private String getBucket() {
        return qiNiuOssConfig.getBucketName();
    }

    private OssInfo getBaseInfo(String targetName) {
        String key = getKey(targetName, false);
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
