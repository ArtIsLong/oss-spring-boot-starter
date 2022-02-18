package io.github.artislong.core.jd;

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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.slice.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
 * https://docs.jdcloud.com/cn/object-storage-service/product-overview
 *
 * @author 陈敏
 * @version JdOssClient.java, v 1.1 2021/11/25 10:44 chenmin Exp $
 * Created on 2021/11/25
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JdOssClient implements StandardOssClient {

    private AmazonS3 amazonS3;
    private TransferManager transferManager;
    private OssProperties ossProperties;
    private JdOssProperties jdOssProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String key = getKey(targetName, false);

        if (isOverride || !amazonS3.doesObjectExist(bucketName, key)) {
            amazonS3.putObject(bucketName, key, is, new ObjectMetadata());
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
    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        upLoadFile(file, targetName);
        return getInfo(targetName);
    }

    public void upLoadFile(File upLoadFile, String targetName) {

        String bucket = getBucket();
        String key = getKey(targetName, false);
        String checkpointFile = upLoadFile.getPath() + StrUtil.DOT + OssConstant.OssType.JD;

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

        SliceConfig slice = getJdOssProperties().getSliceConfig();

        ExecutorService executorService = Executors.newFixedThreadPool(slice.getTaskNum());
        List<Future<PartResult>> futures = new ArrayList<>();

        for (int i = 0; i < upLoadCheckPoint.getUploadParts().size(); i++) {
            if (!upLoadCheckPoint.getUploadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new UploadPartTask(amazonS3, upLoadCheckPoint, i)));
            }
        }

        executorService.shutdown();

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

        try {
            if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new OssException("关闭线程池失败", e);
        }

        List<PartEntityTag> partEntityTags = upLoadCheckPoint.getPartEntityTags();
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(PartEntityTag::getPartNumber))
                .map(partEntityTag -> new PartETag(partEntityTag.getPartNumber(), partEntityTag.getETag())).collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucket, key, upLoadCheckPoint.getUploadId(), eTags);
        amazonS3.completeMultipartUpload(completeMultipartUploadRequest);

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
        uploadCheckPoint.setUploadFileStat(FileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = getJdOssProperties().getSliceConfig().getPartSize();
        long fileLength = upLoadFile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitFile(uploadCheckPoint.getUploadFileStat().getSize(), parts));
        uploadCheckPoint.setPartEntityTags(new ArrayList<>());
        uploadCheckPoint.setOriginPartSize(parts);

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResult result = amazonS3.initiateMultipartUpload(request);

        uploadCheckPoint.setUploadId(result.getUploadId());
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
        AmazonS3 amazonS3;
        UpLoadCheckPoint upLoadCheckPoint;
        int partNum;

        UploadPartTask(AmazonS3 amazonS3, UpLoadCheckPoint upLoadCheckPoint, int partNum) {
            this.amazonS3 = amazonS3;
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

                UploadPartResult uploadPartResponse = amazonS3.uploadPart(uploadPartRequest);

                partResult.setNumber(uploadPartResponse.getPartNumber());

                upLoadCheckPoint.update(partNum, new PartEntityTag().setETag(uploadPartResponse.getETag())
                        .setPartNumber(uploadPartResponse.getPartNumber()), true);
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
        S3Object s3Object = amazonS3.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(s3Object.getObjectContent(), os);
    }

    @Override
    public void delete(String targetName) {
        amazonS3.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String targetKey = getKey(targetName, false);
        if (isOverride || !amazonS3.doesObjectExist(bucketName, targetKey)) {
            amazonS3.copyObject(getBucket(), getKey(sourceName, false), getBucket(), targetKey);
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
            ObjectListing listObjects = amazonS3.listObjects(getBucket(), prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (S3ObjectSummary s3ObjectSummary : listObjects.getObjectSummaries()) {
                if (FileNameUtil.getName(s3ObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(s3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(s3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(s3ObjectSummary.getSize()));
                } else {
                    fileOssInfos.add(getInfo(replaceKey(s3ObjectSummary.getKey(), getBasePath(), false), false));
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
        return amazonS3.doesObjectExist(getBucket(), getKey(targetName, false));
    }

    private String getBucket() {
        return jdOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = amazonS3.getObjectMetadata(getBucket(), replaceKey(key, "", false));
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
