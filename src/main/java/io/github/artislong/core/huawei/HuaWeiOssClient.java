package io.github.artislong.core.huawei;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.*;
import io.github.artislong.OssProperties;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * https://support.huaweicloud.com/obs/index.html
 * @author 陈敏
 * @version HuaWeiOssClient.java, v 1.1 2021/11/25 10:01 chenmin Exp $
 * Created on 2021/11/25
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HuaWeiOssClient implements StandardOssClient {

    private ObsClient obsClient;
    private HuaWeiOssProperties huaWeiOssProperties;
    private OssProperties ossProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !obsClient.doesObjectExist(bucket, key)) {
            obsClient.putObject(bucket, key, is);
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        // 初始化线程池
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        // 初始化分段上传任务
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResult result = obsClient.initiateMultipartUpload(request);

        final String uploadId = result.getUploadId();
        System.out.println("\t" + uploadId + "\n");

        // 每段上传100MB
        long partSize = 100 * 1024 * 1024L;
        long fileSize = file.length();

        // 计算需要上传的段数
        long partCount = fileSize % partSize == 0 ? fileSize / partSize : fileSize / partSize + 1;

        final List<PartEtag> partEtags = Collections.synchronizedList(new ArrayList<PartEtag>());

        // 执行并发上传段
        for (int i = 0; i < partCount; i++) {
            // 分段在文件中的起始位置
            final long offset = i * partSize;
            // 分段大小
            final long currPartSize = (i + 1 == partCount) ? fileSize - offset : partSize;
            // 分段号
            final int partNumber = i + 1;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucket);
                    uploadPartRequest.setObjectKey(key);
                    uploadPartRequest.setUploadId(uploadId);
                    uploadPartRequest.setFile(file);
                    uploadPartRequest.setPartSize(currPartSize);
                    uploadPartRequest.setOffset(offset);
                    uploadPartRequest.setPartNumber(partNumber);

                    UploadPartResult uploadPartResult;
                    try {
                        uploadPartResult = obsClient.uploadPart(uploadPartRequest);
                        System.out.println("Part#" + partNumber + " done\n");
                        partEtags.add(new PartEtag(uploadPartResult.getEtag(), uploadPartResult.getPartNumber()));
                    } catch (ObsException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // 等待上传完成
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 合并段
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucket, key, uploadId, partEtags);
        obsClient.completeMultipartUpload(completeMultipartUploadRequest);
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        ObsObject obsObject = obsClient.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(obsObject.getObjectContent(), os);
    }

    @Override
    public void delete(String targetName) {
        obsClient.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String newTargetName = getKey(targetName, false);
        if (isOverride || !obsClient.doesObjectExist(bucket, newTargetName)) {
            obsClient.copyObject(bucket, getKey(sourceName, false), bucket, newTargetName);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(getBucket());
            listObjectsRequest.setDelimiter("/");
            String prefix = convertPath(key, false);
            listObjectsRequest.setPrefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);

            ObjectListing listObjects = obsClient.listObjects(listObjectsRequest);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (ObsObject obsObject : listObjects.getObjects()) {
                if (FileNameUtil.getName(obsObject.getObjectKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(obsObject.getMetadata().getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(obsObject.getMetadata().getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(obsObject.getMetadata().getContentLength()));
                } else {
                    fileOssInfos.add(getInfo(replaceKey(obsObject.getObjectKey(), getBasePath(), false), false));
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
        return obsClient.doesObjectExist(getBucket(), getKey(targetName, false));
    }

    private String getBucket() {
        return huaWeiOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = obsClient.getObjectMetadata(getBucket(), key);
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
