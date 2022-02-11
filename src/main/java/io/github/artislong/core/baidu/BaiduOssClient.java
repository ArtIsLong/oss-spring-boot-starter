package io.github.artislong.core.baidu;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.*;
import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.model.slice.*;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.SliceConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    private OssProperties ossProperties;
    private BaiduOssProperties baiduOssProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !bosClient.doesObjectExist(bucket, key)) {
            bosClient.putObject(bucket, targetName, is);
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

        String bucket = getBucket();
        String key = getKey(targetName, false);
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

        SliceConfig slice = getBaiduOssProperties().getSliceConfig();

        ExecutorService executorService = Executors.newFixedThreadPool(slice.getTaskNum());
        List<Future<PartResult>> futures = new ArrayList<>();

        for (int i = 0; i < upLoadCheckPoint.getUploadParts().size(); i++) {
            if (!upLoadCheckPoint.getUploadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new UploadPartTask(bosClient, upLoadCheckPoint, i)));
            }
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new OssException("关闭线程池失败", e);
        }

        for (Future<PartResult> future : futures) {
            try {
                PartResult partResult = future.get();
                if (partResult.isFailed()) {
                    throw partResult.getException();
                }
            } catch (Exception e) {
                throw new OssException(e);
            }
        }

        List<PartEntityTag> partEntityTags = upLoadCheckPoint.getPartEntityTags();
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(PartEntityTag::getPartNumber))
                .map(partEntityTag -> {
                    PartETag p = new PartETag();
                    p.setETag(partEntityTag.getETag());
                    p.setPartNumber(partEntityTag.getPartNumber());
                    return p;
                }).collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucket, key, upLoadCheckPoint.getUploadId(), eTags);
        bosClient.completeMultipartUpload(completeMultipartUploadRequest);
    }

    private void prepare(UpLoadCheckPoint uploadCheckPoint, File upLoadFile, String targetName, String checkpointFile) {
        String bucket = getBucket();
        String key = getKey(targetName, false);

        uploadCheckPoint.setMagic(UpLoadCheckPoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(upLoadFile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setBucket(bucket);
        uploadCheckPoint.setCheckpointFile(checkpointFile);
        uploadCheckPoint.setUploadFileStat(FileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = getBaiduOssProperties().getSliceConfig().getPartSize();
        long fileLength = upLoadFile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitFile(uploadCheckPoint.getUploadFileStat().getSize(), parts));
        uploadCheckPoint.setPartEntityTags(new ArrayList<>());
        uploadCheckPoint.setOriginPartSize(parts);

        InitiateMultipartUploadRequest initiateUploadRequest = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResponse initiateMultipartUploadResponse =
                bosClient.initiateMultipartUpload(initiateUploadRequest);

        uploadCheckPoint.setUploadId(initiateMultipartUploadResponse.getUploadId());
    }

    private ArrayList<UploadPart> splitFile(long fileSize, long partSize) {
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

    public static class UploadPartTask implements Callable<PartResult> {
        BosClient bosClient;
        UpLoadCheckPoint upLoadCheckPoint;
        int partNum;

        UploadPartTask(BosClient bosClient, UpLoadCheckPoint upLoadCheckPoint, int partNum) {
            this.bosClient = bosClient;
            this.upLoadCheckPoint = upLoadCheckPoint;
            this.partNum = partNum;
        }

        @Override
        public PartResult call() {
            PartResult partResult = null;
            InputStream inputStream = null;
            try {
                UploadPart uploadPart = upLoadCheckPoint.getUploadParts().get(partNum);

                partResult = new PartResult(partNum + 1, uploadPart.getOffset(), uploadPart.getSize());

                File uploadFile = new File(upLoadCheckPoint.getUploadFile());

                inputStream = new FileInputStream(uploadFile);
                inputStream.skip(uploadPart.getOffset());

                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(upLoadCheckPoint.getBucket());
                uploadPartRequest.setKey(upLoadCheckPoint.getKey());
                uploadPartRequest.setUploadId(upLoadCheckPoint.getUploadId());
                uploadPartRequest.setInputStream(inputStream);
                uploadPartRequest.setPartSize(uploadPart.getSize());
                uploadPartRequest.setPartNumber(uploadPart.getNumber());

                UploadPartResponse uploadPartResponse = bosClient.uploadPart(uploadPartRequest);

                partResult.setNumber(uploadPartResponse.getPartNumber());
                PartETag eTag = uploadPartResponse.getPartETag();

                upLoadCheckPoint.update(partNum, new PartEntityTag().setETag(eTag.getETag())
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
            for (BosObjectSummary bosObjectSummary : listObjects.getContents()) {
                if (FileNameUtil.getName(bosObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(bosObjectSummary.getSize()));
                } else {
                    fileOssInfos.add(getInfo(replaceKey(bosObjectSummary.getKey(), getBasePath(), false), false));
                }
            }

            for (String commonPrefix : listObjects.getCommonPrefixes()) {
                String target = replaceKey(commonPrefix, getBasePath(), false);
                if (isDirectory(commonPrefix)) {
                    directoryInfos.add(getInfo(target, true));
                } else {
                    fileOssInfos.add(getInfo(target, false));
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

    private String getBucket() {
        return baiduOssProperties.getBucketName();
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
