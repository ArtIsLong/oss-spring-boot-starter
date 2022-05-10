package io.github.artislong.core.jd;

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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jd.model.JdOssConfig;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.upload.UpLoadCheckPoint;
import io.github.artislong.model.upload.UpLoadPartEntityTag;
import io.github.artislong.model.upload.UpLoadPartResult;
import io.github.artislong.model.upload.UploadPart;
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

    public static final String AMAZONS3_OBJECT_NAME = "amazonS3";
    public static final String TRANSFER_OBJECT_NAME = "transferManager";

    private AmazonS3 amazonS3;
    private TransferManager transferManager;
    private JdOssConfig jdOssConfig;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String key = getKey(targetName, false);

        if (isOverride || !amazonS3.doesObjectExist(bucketName, key)) {
            amazonS3.putObject(bucketName, key, is, new ObjectMetadata());
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        return uploadFile(file, targetName, jdOssConfig.getSliceConfig(), OssConstant.OssType.JD);
    }

    @Override
    public void completeUpload(UpLoadCheckPoint upLoadCheckPoint, List<UpLoadPartEntityTag> partEntityTags) {
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(UpLoadPartEntityTag::getPartNumber))
                .map(partEntityTag -> new PartETag(partEntityTag.getPartNumber(), partEntityTag.getETag())).collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(upLoadCheckPoint.getBucket(), upLoadCheckPoint.getKey(), upLoadCheckPoint.getUploadId(), eTags);
        amazonS3.completeMultipartUpload(completeMultipartUploadRequest);

        FileUtil.del(upLoadCheckPoint.getCheckpointFile());
    }

    @Override
    public UpLoadPartResult uploadPart(UpLoadCheckPoint upLoadCheckPoint, int partNum, InputStream inputStream) {
        UpLoadPartResult partResult = null;
        UploadPart uploadPart = upLoadCheckPoint.getUploadParts().get(partNum);
        long partSize = uploadPart.getSize();
        partResult = new UpLoadPartResult(partNum + 1, uploadPart.getOffset(), partSize);
        try {
            inputStream.skip(uploadPart.getOffset());

            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(upLoadCheckPoint.getBucket());
            uploadPartRequest.setKey(upLoadCheckPoint.getKey());
            uploadPartRequest.setUploadId(upLoadCheckPoint.getUploadId());
            uploadPartRequest.setInputStream(inputStream);
            uploadPartRequest.setPartSize(partSize);
            uploadPartRequest.setPartNumber(uploadPart.getNumber());

            UploadPartResult uploadPartResponse = amazonS3.uploadPart(uploadPartRequest);

            partResult.setNumber(uploadPartResponse.getPartNumber());
            partResult.setEntityTag(new UpLoadPartEntityTag().setETag(uploadPartResponse.getETag())
                    .setPartNumber(uploadPartResponse.getPartNumber()));
        } catch (Exception e) {
            partResult.setFailed(true);
            partResult.setException(e);
        } finally {
            IoUtil.close(inputStream);
        }

        return partResult;
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        S3Object s3Object = amazonS3.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(s3Object.getObjectContent(), os);
    }

    @Override
    public void downLoadCheckPoint(File localFile, String targetName) {
        downLoadFile(localFile, targetName, jdOssConfig.getSliceConfig(), OssConstant.OssType.BAIDU);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        ObjectMetadata objectMetadata = amazonS3.getObjectMetadata(getBucket(), getKey(targetName, false));
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
            Long partSize = jdOssConfig.getSliceConfig().getPartSize();
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
        GetObjectRequest request = new GetObjectRequest(getBucket(), key);
        request.setRange(start, end);
        S3Object object = amazonS3.getObject(request);
        return object.getObjectContent();
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
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            String prefix = OssPathUtil.convertPath(key, false);
            ObjectListing listObjects = amazonS3.listObjects(getBucket(), prefix.endsWith(StrUtil.SLASH) ? prefix : prefix + StrUtil.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (S3ObjectSummary s3ObjectSummary : listObjects.getObjectSummaries()) {
                if (FileNameUtil.getName(s3ObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(s3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(s3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setLength(Convert.toStr(s3ObjectSummary.getSize()));
                } else {
                    fileOssInfos.add(getInfo(OssPathUtil.replaceKey(s3ObjectSummary.getKey(), getBasePath(), false), false));
                }
            }

            for (String commonPrefix : listObjects.getCommonPrefixes()) {
                String target = OssPathUtil.replaceKey(commonPrefix, getBasePath(), false);
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

    @Override
    public String getBasePath() {
        return jdOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(AMAZONS3_OBJECT_NAME, getAmazonS3());
                put(TRANSFER_OBJECT_NAME, getTransferManager());
            }
        };
    }

    private String getBucket() {
        String bucketName = jdOssConfig.getBucketName();
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }
        return bucketName;
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = amazonS3.getObjectMetadata(getBucket(), OssPathUtil.replaceKey(key, "", false));
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
