package io.github.artislong.core.aws;

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
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.aws.model.AwsOssConfig;
import io.github.artislong.exception.OssException;
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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://docs.aws.amazon.com/s3/
 * @author 陈敏
 * @version AwsOssClient.java, v 1.0 2022/4/1 18:05 chenmin Exp $
 * Created on 2022/4/1
 */
@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class AwsOssClient implements StandardOssClient {

    public static final String S3_OBJECT_NAME = "s3Client";

    private S3Client s3Client;
    private AwsOssConfig ossConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);

        if (isOverride || !isExist(targetName)) {
            try {
                s3Client.putObject(builder -> builder
                        .bucket(bucketName)
                        .key(key), RequestBody.fromInputStream(inputStream, inputStream.available()));
            } catch (IOException e) {
                throw new OssException(e);
            }
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        return uploadFile(file, targetName, ossConfig.getSliceConfig(), OssConstant.OssType.AWS);
    }

    @Override
    public void prepareUpload(UploadCheckpoint uploadCheckPoint, File uploadfile, String targetName, String checkpointFile, SliceConfig slice) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);

        uploadCheckPoint.setMagic(UploadCheckpoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(uploadfile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setBucket(bucketName);
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

        CreateMultipartUploadResponse multipartUpload = s3Client.createMultipartUpload(builder -> builder.bucket(bucketName).key(key));

        uploadCheckPoint.setUploadId(multipartUpload.uploadId());
    }

    @Override
    public UploadPartResult uploadPart(UploadCheckpoint uploadcheckpoint, int partNum, InputStream inputStream) {
        UploadPart uploadPart = uploadcheckpoint.getUploadParts().get(partNum);
        long partSize = uploadPart.getSize();
        int partNumber = partNum + 1;
        UploadPartResult partResult = new UploadPartResult(partNumber, uploadPart.getOffset(), partSize);

        try {
            inputStream.skip(uploadPart.getOffset());
            UploadPartResponse uploadPartResponse = s3Client.uploadPart(builder -> builder.bucket(uploadcheckpoint.getBucket())
                            .key(uploadcheckpoint.getKey())
                            .uploadId(uploadcheckpoint.getUploadId())
                            .partNumber(partNumber)
                            .contentLength(partSize),
                    RequestBody.fromInputStream(inputStream, inputStream.available()));

            partResult.setNumber(partNumber);
            partResult.setEntityTag(new UploadPartEntityTag().setETag(uploadPartResponse.eTag()).setPartNumber(partNumber));
        } catch (Exception e) {
            partResult.setFailed(true);
            partResult.setException(e);
        } finally {
            IoUtil.close(inputStream);
        }

        return partResult;
    }

    @Override
    public void completeUpload(UploadCheckpoint uploadcheckpoint, List<UploadPartEntityTag> partEntityTags) {
        s3Client.completeMultipartUpload(builder -> builder
                .bucket(uploadcheckpoint.getBucket())
                .key(uploadcheckpoint.getKey())
                .uploadId(uploadcheckpoint.getUploadId()));
        FileUtil.del(uploadcheckpoint.getCheckpointFile());
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {
        ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(builder -> builder
                .bucket(getBucketName())
                .key(getKey(targetName, false)));
        IoUtil.copy(responseInputStream, outputStream);
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        downloadfile(localFile, targetName, ossConfig.getSliceConfig(), OssConstant.OssType.AWS);
    }

    @Override
    public void prepareDownload(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) {
        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);
        downloadCheckPoint.setDownloadFile(localFile.getPath());
        downloadCheckPoint.setBucketName(getBucketName());
        downloadCheckPoint.setKey(getKey(targetName, false));
        downloadCheckPoint.setCheckPointFile(checkpointFile);

        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));

        long downloadSize;
        if (downloadCheckPoint.getObjectStat().getSize() > 0) {
            Long partSize = ossConfig.getSliceConfig().getPartSize();
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
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        GetObjectAttributesResponse objectAttributes = s3Client.getObjectAttributes(builder -> builder
                .key(getKey(targetName, false))
                .bucket(getBucketName()));

        DateTime date = DateUtil.date(objectAttributes.lastModified().getEpochSecond());
        long contentLength = objectAttributes.objectSize();
        String eTag = objectAttributes.eTag();
        return new DownloadObjectStat().setSize(contentLength).setLastModified(date).setDigest(eTag);
    }

    @Override
    public InputStream downloadPart(String key, long start, long end) throws Exception {
        return s3Client.getObject(builder -> builder
                .key(key)
                .bucket(getBucketName())
                .range("bytes=" + start + "-" + end));
    }

    @Override
    public void delete(String targetName) {
        s3Client.deleteObject(builder -> builder.key(getKey(targetName, false)).bucket(getBucketName()));
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        String bucket = getBucketName();
        if (isOverride || !isExist(targetName)) {
            s3Client.copyObject(builder -> builder
                    .sourceBucket(bucket)
                    .sourceKey(getKey(sourceName, false))
                    .destinationBucket(bucket)
                    .destinationKey(getKey(targetName, false)));
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
            ListObjectsResponse listObjects = s3Client.listObjects(builder -> builder.bucket(getBucketName()).prefix(prefix.endsWith("/") ? prefix : prefix + StrUtil.SLASH));

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(listObjects.contents())) {
                for (S3Object s3Object : listObjects.contents()) {
                    if (FileNameUtil.getName(s3Object.key()).equals(FileNameUtil.getName(key))) {
                        ossInfo.setLastUpdateTime(DateUtil.date(s3Object.lastModified().getEpochSecond()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setCreateTime(DateUtil.date(s3Object.lastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setLength(Convert.toStr(s3Object.size()));
                    } else {
                        fileOssInfos.add(getInfo(OssPathUtil.replaceKey(s3Object.key(), getBasePath(), false), false));
                    }
                }
            }

            if (ObjectUtil.isNotEmpty(listObjects.commonPrefixes())) {
                for (CommonPrefix commonPrefix : listObjects.commonPrefixes()) {
                    String target = OssPathUtil.replaceKey(commonPrefix.prefix(), getBasePath(), false);
                    if (isDirectory(commonPrefix.prefix())) {
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
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(S3_OBJECT_NAME, getS3Client());
            }
        };
    }

    @Override
    public String getBasePath() {
        return ossConfig.getBasePath();
    }

    public String getBucketName() {
        return ossConfig.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                GetObjectAttributesResponse objectAttributes = s3Client.getObjectAttributes(builder -> builder
                        .key(key)
                        .bucket(getBucketName()));

                DateTime date = DateUtil.date(objectAttributes.lastModified().getEpochSecond());
                long contentLength = objectAttributes.objectSize();
                ossInfo.setLastUpdateTime(DateUtil.date(date).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.date(date).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setLength(Convert.toStr(contentLength));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        return ossInfo;
    }
}
