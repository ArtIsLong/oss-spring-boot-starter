package io.github.artislong.core.jinshan;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.ksyun.ks3.dto.*;
import com.ksyun.ks3.service.Ks3;
import com.ksyun.ks3.service.request.CompleteMultipartUploadRequest;
import com.ksyun.ks3.service.request.GetObjectRequest;
import com.ksyun.ks3.service.request.InitiateMultipartUploadRequest;
import com.ksyun.ks3.service.request.UploadPartRequest;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jinshan.model.JinShanOssConfig;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.upload.*;
import io.github.artislong.utils.OssPathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * https://docs.ksyun.com/documents/38731
 * @author 陈敏
 * @version JinShanOssClient.java, v 1.1 2022/3/3 18:00 chenmin Exp $
 * Created on 2022/3/3
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JinShanOssClient implements StandardOssClient {

    public static final String KS3_OBJECT_NAME = "ks3";

    private Ks3 ks3;
    private JinShanOssConfig jinShanOssConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !ks3.objectExists(bucket, key)) {
            ks3.putObject(bucket, key, inputStream, null);
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        return uploadFile(file, targetName, jinShanOssConfig.getSliceConfig(), OssConstant.OssType.JINSHAN);
    }

    @Override
    public void completeUpload(UploadCheckpoint uploadcheckpoint, List<UploadPartEntityTag> partEntityTags) {
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(UploadPartEntityTag::getPartNumber))
                .map(partEntityTag -> {
                    PartETag p = new PartETag();
                    p.seteTag(partEntityTag.getETag());
                    p.setPartNumber(partEntityTag.getPartNumber());
                    return p;
                }).collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(uploadcheckpoint.getBucket(), uploadcheckpoint.getKey(), uploadcheckpoint.getUploadId(), eTags);
        ks3.completeMultipartUpload(completeMultipartUploadRequest);
        FileUtil.del(uploadcheckpoint.getCheckpointFile());
    }

    @Override
    public void prepareUpload(UploadCheckpoint uploadCheckPoint, File uploadfile, String targetName, String checkpointFile, SliceConfig slice) {
        String bucket = getBucket();
        String key = getKey(targetName, false);

        uploadCheckPoint.setMagic(UploadCheckpoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(uploadfile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setBucket(bucket);
        uploadCheckPoint.setCheckpointFile(checkpointFile);
        uploadCheckPoint.setUploadFileStat(UploadFileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = slice.getPartSize();
        long fileLength = uploadfile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitUploadFile(uploadCheckPoint.getUploadFileStat().getSize(), partSize));
        uploadCheckPoint.setPartEntityTags(new ArrayList<>());
        uploadCheckPoint.setOriginPartSize(parts);

        InitiateMultipartUploadResult initiateMultipartUploadResult =
                ks3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key));

        uploadCheckPoint.setUploadId(initiateMultipartUploadResult.getUploadId());
    }

    @Override
    public UploadPartResult uploadPart(UploadCheckpoint uploadcheckpoint, int partNum, InputStream inputStream) {
        UploadPart uploadPart = uploadcheckpoint.getUploadParts().get(partNum);
        long partSize = uploadPart.getSize();
        UploadPartResult partResult = new UploadPartResult(partNum + 1, uploadPart.getOffset(), partSize);

        try {
            inputStream.skip(uploadPart.getOffset());

            UploadPartRequest uploadPartRequest = new UploadPartRequest(uploadcheckpoint.getBucket(), uploadcheckpoint.getKey());
            uploadPartRequest.setUploadId(uploadcheckpoint.getUploadId());
            uploadPartRequest.setInputStream(inputStream);
            uploadPartRequest.setPartSize(partSize);
            uploadPartRequest.setPartNumber(partNum + 1);
            PartETag eTag = ks3.uploadPart(uploadPartRequest);

            partResult.setNumber(eTag.getPartNumber());
            partResult.setEntityTag(new UploadPartEntityTag().setETag(eTag.geteTag()).setPartNumber(eTag.getPartNumber()));
        } catch (Exception e) {
            partResult.setFailed(true);
            partResult.setException(e);
        } finally {
            IoUtil.close(inputStream);
        }

        return partResult;
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {
        GetObjectResult objectResult = ks3.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(objectResult.getObject().getObjectContent(), outputStream);
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        downloadfile(localFile, targetName, jinShanOssConfig.getSliceConfig(), OssConstant.OssType.JINSHAN);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        GetObjectResult objectResult = ks3.getObject(getBucket(), getKey(targetName, false));
        ObjectMetadata objectMetadata = objectResult.getObject().getObjectMetadata();
        DateTime date = DateUtil.date(objectMetadata.getLastModified());
        long contentLength = objectMetadata.getContentLength();
        String eTag = objectMetadata.getETag();
        return new DownloadObjectStat().setSize(contentLength).setLastModified(date).setDigest(eTag);
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
            Long partSize = jinShanOssConfig.getSliceConfig().getPartSize();
            long[] slice = getDownloadSlice(new long[0], downloadCheckPoint.getObjectStat().getSize());
            downloadCheckPoint.setDownloadParts(splitDownloadFile(slice[0], slice[1], partSize));
            downloadSize = slice[1];
        } else {
            downloadSize = 0;
            downloadCheckPoint.setDownloadParts(splitDownloadOneFile());
        }
        downloadCheckPoint.setOriginPartSize(downloadCheckPoint.getDownloadParts().size());
        createDownloadTemp(downloadCheckPoint.getTempDownloadFile(), downloadSize);
    }

    @Override
    public InputStream downloadPart(String key, long start, long end) {
        GetObjectRequest request = new GetObjectRequest(getBucket(), key);
        request.setRange(start, end);
        GetObjectResult object = ks3.getObject(request);
        return object.getObject().getObjectContent();
    }

    @Override
    public void delete(String targetName) {
        ks3.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        String bucket = getBucket();
        String newTargetName = getKey(targetName, false);
        if (isOverride || !ks3.objectExists(bucket, newTargetName)) {
            ks3.copyObject(bucket, newTargetName, bucket, getKey(sourceName, false));
        }
    }

    @Override
    public OssInfo getInfo(String targetName, boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            String prefix = OssPathUtil.convertPath(key, false);
            ObjectListing listObjects = ks3.listObjects(getBucket(), prefix.endsWith(StrUtil.SLASH) ? prefix : prefix + StrUtil.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(listObjects.getObjectSummaries())) {
                for (Ks3ObjectSummary ks3ObjectSummary : listObjects.getObjectSummaries()) {
                    if (FileNameUtil.getName(ks3ObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                        ossInfo.setLastUpdateTime(DateUtil.date(ks3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setCreateTime(DateUtil.date(ks3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setLength(Convert.toStr(ks3ObjectSummary.getSize()));
                    } else {
                        fileOssInfos.add(getInfo(OssPathUtil.replaceKey(ks3ObjectSummary.getKey(), getBasePath(), false), false));
                    }
                }
            }

            if (ObjectUtil.isNotEmpty(listObjects.getCommonPrefixes())) {
                for (String commonPrefix : listObjects.getCommonPrefixes()) {
                    String target = OssPathUtil.replaceKey(commonPrefix, getBasePath(), false);
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
    public boolean isExist(String targetName) {
        return ks3.objectExists(getBucket(), getKey(targetName, false));
    }

    @Override
    public String getBasePath() {
        return jinShanOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(KS3_OBJECT_NAME, getKs3());
            }
        };
    }

    private String getBucket() {
        String bucketName = jinShanOssConfig.getBucketName();
        if (!ks3.bucketExists(bucketName)) {
            ks3.createBucket(bucketName);
        }
        return bucketName;
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                GetObjectResult objectResult = ks3.getObject(getBucket(), key);
                ObjectMetadata objectMetadata = objectResult.getObject().getObjectMetadata();
                ossInfo.setLastUpdateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setLength(Convert.toStr(objectMetadata.getContentLength()));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        return ossInfo;
    }

}
