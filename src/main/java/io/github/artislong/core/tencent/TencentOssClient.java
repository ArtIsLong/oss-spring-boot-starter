package io.github.artislong.core.tencent;

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
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.tencent.model.TencentOssConfig;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.upload.UploadPartResult;
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
 * https://cloud.tencent.com/document/product/436
 * @author 陈敏
 * @version TencentOssClient.java, v 1.1 2021/11/24 15:35 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TencentOssClient implements StandardOssClient {

    public static final String COS_OBJECT_NAME = "cosClient";

    private COSClient cosClient;
    private TencentOssConfig tencentOssConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        String bucketName = getBucket();
        String key = getKey(targetName, false);

        if (isOverride || !cosClient.doesObjectExist(bucketName, key)) {
            cosClient.putObject(bucketName, key, inputStream, new ObjectMetadata());
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        uploadFile(file, targetName, tencentOssConfig.getSliceConfig(), OssType.TENCENT);
        return getInfo(targetName);
    }

    @Override
    public void completeUpload(UploadCheckpoint uploadcheckpoint, List<UploadPartEntityTag> partEntityTags) {
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(UploadPartEntityTag::getPartNumber))
                .map(partEntityTag -> new PartETag(partEntityTag.getPartNumber(), partEntityTag.getETag())).collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(uploadcheckpoint.getBucket(), uploadcheckpoint.getKey(), uploadcheckpoint.getUploadId(), eTags);
        cosClient.completeMultipartUpload(completeMultipartUploadRequest);

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

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResult result = cosClient.initiateMultipartUpload(request);

        uploadCheckPoint.setUploadId(result.getUploadId());
    }

    @Override
    public UploadPartResult uploadPart(UploadCheckpoint uploadcheckpoint, int partNum, InputStream inputStream) {
        UploadPartResult partResult = null;
        UploadPart uploadPart = uploadcheckpoint.getUploadParts().get(partNum);
        long partSize = uploadPart.getSize();
        partResult = new UploadPartResult(partNum + 1, uploadPart.getOffset(), partSize);
        try {
            inputStream.skip(uploadPart.getOffset());

            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(uploadcheckpoint.getBucket());
            uploadPartRequest.setKey(uploadcheckpoint.getKey());
            uploadPartRequest.setUploadId(uploadcheckpoint.getUploadId());
            uploadPartRequest.setInputStream(inputStream);
            uploadPartRequest.setPartSize(partSize);
            uploadPartRequest.setPartNumber(uploadPart.getNumber());

            com.qcloud.cos.model.UploadPartResult uploadPartResponse = cosClient.uploadPart(uploadPartRequest);

            partResult.setNumber(uploadPartResponse.getPartNumber());
            partResult.setEntityTag(new UploadPartEntityTag().setETag(uploadPartResponse.getETag())
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
    public void download(OutputStream outputStream, String targetName) {
        COSObject cosObject = cosClient.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(cosObject.getObjectContent(), outputStream);
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        downloadfile(localFile, targetName, tencentOssConfig.getSliceConfig(), OssType.TENCENT);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        ObjectMetadata objectMetadata = cosClient.getObjectMetadata(getBucket(), getKey(targetName, false));
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
            Long partSize = tencentOssConfig.getSliceConfig().getPartSize();
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
        COSObject object = cosClient.getObject(request);
        return object.getObjectContent();
    }

    @Override
    public void delete(String targetName) {
        cosClient.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        String bucketName = getBucket();
        String targetKey = getKey(targetName, false);
        if (isOverride || !cosClient.doesObjectExist(bucketName, targetKey)) {
            cosClient.copyObject(getBucket(), getKey(sourceName, false), getBucket(), targetKey);
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
            ObjectListing listObjects = cosClient.listObjects(getBucket(), prefix.endsWith(StrUtil.SLASH) ? prefix : prefix + StrUtil.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (COSObjectSummary cosObjectSummary : listObjects.getObjectSummaries()) {
                if (FileNameUtil.getName(cosObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(cosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(cosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setLength(Convert.toStr(cosObjectSummary.getSize()));
                } else {
                    fileOssInfos.add(getInfo(OssPathUtil.replaceKey(cosObjectSummary.getKey(), getBasePath(), false), false));
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
    public boolean isExist(String targetName) {
        return cosClient.doesObjectExist(getBucket(), getKey(targetName, false));
    }

    @Override
    public String getBasePath() {
        return tencentOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(COS_OBJECT_NAME, getCosClient());
            }
        };
    }

    private String getBucket() {
        String bucketName = tencentOssConfig.getBucketName();
        if (!cosClient.doesBucketExist(bucketName)) {
            cosClient.createBucket(bucketName);
        }
        return bucketName;
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = cosClient.getObjectMetadata(getBucket(), key);
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
