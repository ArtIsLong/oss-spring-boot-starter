package io.github.artislong.core.baidu;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.*;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.baidu.model.BaiduOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.download.DownloadPart;
import io.github.artislong.model.download.DownloadPartResult;
import io.github.artislong.model.upload.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.aliyun.oss.internal.OSSConstants.DEFAULT_BUFFER_SIZE;

/**
 * https://cloud.baidu.com/doc/BOS/index.html
 *
 * @author 陈敏
 * @version BaiduOssClient.java, v 1.1 2021/11/24 15:34 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaiduOssClient implements StandardOssClient {

    private BosClient bosClient;
    private BaiduOssConfig baiduOssConfig;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !bosClient.doesObjectExist(bucket, key)) {
            bosClient.putObject(bucket, key, is);
        }
        return getInfo(targetName);
    }

    /**
     * 断点续传，通过分块上传实现
     *
     * @param file       本地文件
     * @param targetName 目标文件路径
     * @return
     */
    @SneakyThrows
    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        upLoadFile(file, targetName);
        return getInfo(targetName);
    }

    public void upLoadFile(File upLoadFile, String targetName) {

        String checkpointFile = upLoadFile.getPath() + StrUtil.DOT + OssConstant.OssType.BAIDU;

        UpLoadCheckPoint upLoadCheckPoint = new UpLoadCheckPoint();
        try {
            upLoadCheckPoint.load(checkpointFile);
        } catch (Exception e) {
            FileUtil.del(checkpointFile);
        }

        if (!upLoadCheckPoint.isValid()) {
            prepare(upLoadCheckPoint, upLoadFile, targetName, checkpointFile);
            FileUtil.del(checkpointFile);
        }

        SliceConfig slice = baiduOssConfig.getSliceConfig();

        ExecutorService executorService = Executors.newFixedThreadPool(slice.getTaskNum());
        List<Future<UpLoadPartResult>> futures = new ArrayList<>();

        for (int i = 0; i < upLoadCheckPoint.getUploadParts().size(); i++) {
            if (!upLoadCheckPoint.getUploadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new UploadPartTask(bosClient, upLoadCheckPoint, i)));
            }
        }

        executorService.shutdown();

        for (Future<UpLoadPartResult> future : futures) {
            try {
                UpLoadPartResult partResult = future.get();
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

        List<UpLoadPartEntityTag> partEntityTags = upLoadCheckPoint.getPartEntityTags();
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(UpLoadPartEntityTag::getPartNumber))
                .map(partEntityTag -> {
                    PartETag p = new PartETag();
                    p.setETag(partEntityTag.getETag());
                    p.setPartNumber(partEntityTag.getPartNumber());
                    return p;
                }).collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(upLoadCheckPoint.getBucket(), upLoadCheckPoint.getKey(), upLoadCheckPoint.getUploadId(), eTags);
        bosClient.completeMultipartUpload(completeMultipartUploadRequest);

        FileUtil.del(checkpointFile);
    }

    private void prepare(UpLoadCheckPoint uploadCheckPoint, File upLoadFile, String targetName, String checkpointFile) {
        String bucket = getBucket();
        String key = getKey(targetName, false);

        uploadCheckPoint.setMagic(UpLoadCheckPoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(upLoadFile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setBucket(bucket);
        uploadCheckPoint.setCheckpointFile(checkpointFile);
        uploadCheckPoint.setUploadFileStat(UpLoadFileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = baiduOssConfig.getSliceConfig().getPartSize();
        long fileLength = upLoadFile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitUploadFile(uploadCheckPoint.getUploadFileStat().getSize(), parts));
        uploadCheckPoint.setPartEntityTags(new ArrayList<>());
        uploadCheckPoint.setOriginPartSize(parts);

        InitiateMultipartUploadResponse initiateMultipartUploadResponse =
                bosClient.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key));

        uploadCheckPoint.setUploadId(initiateMultipartUploadResponse.getUploadId());
    }

    private ArrayList<UploadPart> splitUploadFile(long fileSize, long partSize) {
        ArrayList<UploadPart> parts = new ArrayList<>();

        long partNum = fileSize / partSize;
        if (partNum >= 10000) {
            partSize = fileSize / (10000 - 1);
            partNum = fileSize / partSize;
        }

        for (long i = 0; i < partNum; i++) {
            UploadPart part = new UploadPart();
            part.setNumber((int) (i + 1));
            part.setOffset(i * partSize);
            part.setSize(partSize);
            part.setCompleted(false);
            parts.add(part);
        }

        if (fileSize % partSize > 0) {
            UploadPart part = new UploadPart();
            part.setNumber(parts.size() + 1);
            part.setOffset(parts.size() * partSize);
            part.setSize(fileSize % partSize);
            part.setCompleted(false);
            parts.add(part);
        }

        return parts;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadPartTask implements Callable<UpLoadPartResult> {
        BosClient bosClient;
        UpLoadCheckPoint upLoadCheckPoint;
        int partNum;

        @Override
        public UpLoadPartResult call() {
            UpLoadPartResult partResult = null;
            InputStream inputStream = null;
            try {
                UploadPart uploadPart = upLoadCheckPoint.getUploadParts().get(partNum);
                long partSize = uploadPart.getSize();

                partResult = new UpLoadPartResult(partNum + 1, uploadPart.getOffset(), partSize);

                inputStream = new FileInputStream(upLoadCheckPoint.getUploadFile());
                inputStream.skip(uploadPart.getOffset());

                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(upLoadCheckPoint.getBucket());
                uploadPartRequest.setKey(upLoadCheckPoint.getKey());
                uploadPartRequest.setUploadId(upLoadCheckPoint.getUploadId());
                uploadPartRequest.setInputStream(inputStream);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setPartNumber(partNum + 1);
                UploadPartResponse uploadPartResponse = bosClient.uploadPart(uploadPartRequest);

                partResult.setNumber(uploadPartResponse.getPartNumber());
                PartETag eTag = uploadPartResponse.getPartETag();

                upLoadCheckPoint.update(partNum, new UpLoadPartEntityTag().setETag(eTag.getETag())
                        .setPartNumber(eTag.getPartNumber()), true);
                upLoadCheckPoint.dump();
            } catch (Exception e) {
                partResult.setFailed(true);
                partResult.setException(e);
            } finally {
                IoUtil.close(inputStream);
            }

            return partResult;
        }
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        BosObject bosObject = bosClient.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(bosObject.getObjectContent(), os);
    }

    @Override
    public void downLoadCheckPoint(File localFile, String targetName) {

        String checkpointFile = localFile.getPath() + StrUtil.DOT + OssConstant.OssType.BAIDU;

        DownloadCheckPoint downloadCheckPoint = new DownloadCheckPoint();
        try {
            downloadCheckPoint.load(checkpointFile);
        } catch (Exception e) {
            FileUtil.del(checkpointFile);
        }

        DownloadObjectStat downloadObjectStat = getDownloadObjectStat(targetName);
        if (!downloadCheckPoint.isValid(downloadObjectStat)) {
            prepare(downloadCheckPoint, localFile, targetName, checkpointFile);
            FileUtil.del(checkpointFile);
        }

        SliceConfig slice = baiduOssConfig.getSliceConfig();

        ExecutorService executorService = Executors.newFixedThreadPool(slice.getTaskNum());
        List<Future<DownloadPartResult>> futures = new ArrayList<>();

        for (int i = 0; i < downloadCheckPoint.getDownloadParts().size(); i++) {
            if (!downloadCheckPoint.getDownloadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new DownloadPartTask(bosClient, downloadCheckPoint, i)));
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

    private DownloadObjectStat getDownloadObjectStat(String targetName) {
        ObjectMetadata objectMetadata = bosClient.getObjectMetadata(getBucket(), getKey(targetName, false));
        DateTime date = DateUtil.date(objectMetadata.getLastModified());
        long contentLength = objectMetadata.getContentLength();
        String eTag = objectMetadata.getETag();
        return new DownloadObjectStat().setSize(contentLength).setLastModified(date).setDigest(eTag);
    }

    private void prepare(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) {
        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);
        downloadCheckPoint.setDownloadFile(localFile.getPath());
        downloadCheckPoint.setBucketName(getBucket());
        downloadCheckPoint.setObjectKey(getKey(targetName, false));
        downloadCheckPoint.setCheckPointFile(checkpointFile);

        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));

        long downloadSize;
        if (downloadCheckPoint.getObjectStat().getSize() > 0) {
            Long partSize = baiduOssConfig.getSliceConfig().getPartSize();
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
        if (partNum >= 10000) {
            partSize = objectSize / (10000 - 1);
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
                (range[0] > 0 && range[1] > 0 && range[0] > range[1])||
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
                end = totalSize -1;
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

        BosClient bosClient;
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

                GetObjectRequest request = new GetObjectRequest();
                request.setKey(downloadCheckPoint.getObjectKey());
                request.setBucketName(downloadCheckPoint.getBucketName());
                request.setRange(downloadPart.getStart(), downloadPart.getEnd());
                BosObject object = bosClient.getObject(request);
                content = object.getObjectContent();

                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int bytesRead = 0;
                while ((bytesRead = content.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                partResult.setLength(downloadPart.getLength());
                downloadCheckPoint.update(partNum, true);
                downloadCheckPoint.dump(downloadCheckPoint.getCheckPointFile());
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

    @Override
    public void delete(String targetName) {
        bosClient.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String newTargetName = getKey(targetName, false);
        if (isOverride || !bosClient.doesObjectExist(bucket, newTargetName)) {
            bosClient.copyObject(bucket, getKey(sourceName, false), bucket, newTargetName);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            String prefix = convertPath(key, false);
            ListObjectsResponse listObjects = bosClient.listObjects(getBucket(), prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(listObjects.getContents())) {
                for (BosObjectSummary bosObjectSummary : listObjects.getContents()) {
                    if (FileNameUtil.getName(bosObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                        ossInfo.setLastUpdateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setCreateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setSize(Convert.toStr(bosObjectSummary.getSize()));
                    } else {
                        fileOssInfos.add(getInfo(replaceKey(bosObjectSummary.getKey(), getBasePath(), false), false));
                    }
                }
            }

            if (ObjectUtil.isNotEmpty(listObjects.getCommonPrefixes())) {
                for (String commonPrefix : listObjects.getCommonPrefixes()) {
                    String target = replaceKey(commonPrefix, getBasePath(), false);
                    if (isDirectory(commonPrefix)) {
                        directoryInfos.add(getInfo(target, true));
                    } else {
                        fileOssInfos.add(getInfo(target, false));
                    }
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
    public Boolean isExist(String targetName) {
        return bosClient.doesObjectExist(getBucket(), getKey(targetName, false));
    }

    @Override
    public String getBasePath() {
        return baiduOssConfig.getBasePath();
    }

    private String getBucket() {
        return baiduOssConfig.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = bosClient.getObjectMetadata(getBucket(), key);
                ossInfo.setLastUpdateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setSize(Convert.toStr(objectMetadata.getContentLength()));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        return ossInfo;
    }

}
