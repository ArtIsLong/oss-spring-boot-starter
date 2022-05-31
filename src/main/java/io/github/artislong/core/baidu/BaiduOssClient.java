package io.github.artislong.core.baidu;

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
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.*;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.baidu.model.BaiduOssConfig;
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

    public static final String BOS_OBJECT_NAME = "bosClient";

    private BosClient bosClient;
    private BaiduOssConfig baiduOssConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !bosClient.doesObjectExist(bucket, key)) {
            bosClient.putObject(bucket, key, inputStream);
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        return uploadFile(file, targetName, baiduOssConfig.getSliceConfig(), OssConstant.OssType.BAIDU);
    }

    @Override
    public void completeUpload(UploadCheckpoint uploadcheckpoint, List<UploadPartEntityTag> partEntityTags) {
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(UploadPartEntityTag::getPartNumber))
                .map(partEntityTag -> {
                    PartETag p = new PartETag();
                    p.setETag(partEntityTag.getETag());
                    p.setPartNumber(partEntityTag.getPartNumber());
                    return p;
                }).collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(uploadcheckpoint.getBucket(), uploadcheckpoint.getKey(), uploadcheckpoint.getUploadId(), eTags);
        bosClient.completeMultipartUpload(completeMultipartUploadRequest);
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

        InitiateMultipartUploadResponse initiateMultipartUploadResponse =
                bosClient.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key));

        uploadCheckPoint.setUploadId(initiateMultipartUploadResponse.getUploadId());
    }

    @Override
    public UploadPartResult uploadPart(UploadCheckpoint uploadcheckpoint, int partNum, InputStream inputStream) {
        UploadPart uploadPart = uploadcheckpoint.getUploadParts().get(partNum);
        long partSize = uploadPart.getSize();
        UploadPartResult partResult = new UploadPartResult(partNum + 1, uploadPart.getOffset(), partSize);

        try {
            inputStream.skip(uploadPart.getOffset());

            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(uploadcheckpoint.getBucket());
            uploadPartRequest.setKey(uploadcheckpoint.getKey());
            uploadPartRequest.setUploadId(uploadcheckpoint.getUploadId());
            uploadPartRequest.setInputStream(inputStream);
            uploadPartRequest.setPartSize(partSize);
            uploadPartRequest.setPartNumber(partNum + 1);
            UploadPartResponse uploadPartResponse = bosClient.uploadPart(uploadPartRequest);

            partResult.setNumber(uploadPartResponse.getPartNumber());
            PartETag eTag = uploadPartResponse.getPartETag();
            partResult.setEntityTag(new UploadPartEntityTag().setETag(eTag.getETag()).setPartNumber(eTag.getPartNumber()));
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
        BosObject bosObject = bosClient.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(bosObject.getObjectContent(), outputStream);
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        downloadfile(localFile, targetName, baiduOssConfig.getSliceConfig(), OssConstant.OssType.BAIDU);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        ObjectMetadata objectMetadata = bosClient.getObjectMetadata(getBucket(), getKey(targetName, false));
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
            Long partSize = baiduOssConfig.getSliceConfig().getPartSize();
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
        GetObjectRequest request = new GetObjectRequest();
        request.setKey(key);
        request.setBucketName(getBucket());
        request.setRange(start, end);
        BosObject object = bosClient.getObject(request);
        return object.getObjectContent();
    }

    @Override
    public void delete(String targetName) {
        bosClient.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        String bucket = getBucket();
        String newTargetName = getKey(targetName, false);
        if (isOverride || !bosClient.doesObjectExist(bucket, newTargetName)) {
            bosClient.copyObject(bucket, getKey(sourceName, false), bucket, newTargetName);
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
            ListObjectsResponse listObjects = bosClient.listObjects(getBucket(), prefix.endsWith(StrUtil.SLASH) ? prefix : prefix + StrUtil.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(listObjects.getContents())) {
                for (BosObjectSummary bosObjectSummary : listObjects.getContents()) {
                    if (FileNameUtil.getName(bosObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                        ossInfo.setLastUpdateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setCreateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                        ossInfo.setLength(Convert.toStr(bosObjectSummary.getSize()));
                    } else {
                        fileOssInfos.add(getInfo(OssPathUtil.replaceKey(bosObjectSummary.getKey(), getBasePath(), false), false));
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
        return bosClient.doesObjectExist(getBucket(), getKey(targetName, false));
    }

    @Override
    public String getBasePath() {
        return baiduOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(BOS_OBJECT_NAME, getBosClient());
            }
        };
    }

    private String getBucket() {
        String bucketName = baiduOssConfig.getBucketName();
        if (!bosClient.doesBucketExist(bucketName)) {
            bosClient.createBucket(bucketName);
        }
        return bucketName;
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = bosClient.getObjectMetadata(getBucket(), key);
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
