/** * $Id: WangYiOssClient.java,v 1.0 2022/3/4 9:49 PM chenmin Exp $ */package io.github.artislong.core.wangyi;import cn.hutool.core.convert.Convert;import cn.hutool.core.date.DatePattern;import cn.hutool.core.date.DateTime;import cn.hutool.core.date.DateUtil;import cn.hutool.core.io.FileUtil;import cn.hutool.core.io.IoUtil;import cn.hutool.core.io.file.FileNameUtil;import cn.hutool.core.text.CharPool;import cn.hutool.core.util.ObjectUtil;import cn.hutool.core.util.ReflectUtil;import cn.hutool.core.util.StrUtil;import com.netease.cloud.services.nos.NosClient;import com.netease.cloud.services.nos.model.*;import io.github.artislong.constant.OssConstant;import io.github.artislong.core.StandardOssClient;import io.github.artislong.core.wangyi.model.WangYiOssConfig;import io.github.artislong.model.DirectoryOssInfo;import io.github.artislong.model.FileOssInfo;import io.github.artislong.model.OssInfo;import io.github.artislong.model.SliceConfig;import io.github.artislong.model.download.DownloadCheckPoint;import io.github.artislong.model.download.DownloadObjectStat;import io.github.artislong.model.upload.*;import lombok.AllArgsConstructor;import lombok.Data;import lombok.NoArgsConstructor;import lombok.extern.slf4j.Slf4j;import java.io.File;import java.io.InputStream;import java.io.OutputStream;import java.util.*;import java.util.stream.Collectors;/** * @author 陈敏 * @version $Id: WangYiOssClient.java,v 1.1 2022/3/4 9:49 PM chenmin Exp $ * Created on 2022/3/4 9:49 PM * My blog： https://www.chenmin.info */@Slf4j@Data@AllArgsConstructor@NoArgsConstructorpublic class WangYiOssClient implements StandardOssClient {    public static final String NOS_OBJECT_NAME = "nosClient";    private NosClient nosClient;    private WangYiOssConfig wangYiOssConfig;    @Override    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {        String bucket = getBucket();        String key = getKey(targetName, false);        if (isOverride || !nosClient.doesObjectExist(bucket, key, null)) {            nosClient.putObject(bucket, key, is, null);        }        return getInfo(targetName);    }    @Override    public OssInfo upLoadCheckPoint(File file, String targetName) {        return uploadFile(file, targetName, wangYiOssConfig.getSliceConfig(), OssConstant.OssType.WANGYI);    }    @Override    public void completeUpload(UpLoadCheckPoint upLoadCheckPoint, List<UpLoadPartEntityTag> partEntityTags) {        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(UpLoadPartEntityTag::getPartNumber))                .map(partEntityTag -> new PartETag(partEntityTag.getPartNumber(), partEntityTag.getETag())).collect(Collectors.toList());        CompleteMultipartUploadRequest completeMultipartUploadRequest =                new CompleteMultipartUploadRequest(upLoadCheckPoint.getBucket(), upLoadCheckPoint.getKey(), upLoadCheckPoint.getUploadId(), eTags);        nosClient.completeMultipartUpload(completeMultipartUploadRequest);        FileUtil.del(upLoadCheckPoint.getCheckpointFile());    }    @Override    public void prepareUpload(UpLoadCheckPoint uploadCheckPoint, File upLoadFile, String targetName, String checkpointFile, SliceConfig slice) {        String bucket = getBucket();        String key = getKey(targetName, false);        uploadCheckPoint.setMagic(UpLoadCheckPoint.UPLOAD_MAGIC);        uploadCheckPoint.setUploadFile(upLoadFile.getPath());        uploadCheckPoint.setKey(key);        uploadCheckPoint.setBucket(bucket);        uploadCheckPoint.setCheckpointFile(checkpointFile);        uploadCheckPoint.setUploadFileStat(UpLoadFileStat.getFileStat(uploadCheckPoint.getUploadFile()));        long partSize = slice.getPartSize();        long fileLength = upLoadFile.length();        int parts = (int) (fileLength / partSize);        if (fileLength % partSize > 0) {            parts++;        }        uploadCheckPoint.setUploadParts(splitUploadFile(uploadCheckPoint.getUploadFileStat().getSize(), partSize));        uploadCheckPoint.setPartEntityTags(new ArrayList<>());        uploadCheckPoint.setOriginPartSize(parts);        InitiateMultipartUploadResult initiateMultipartUploadResult =                nosClient.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key));        uploadCheckPoint.setUploadId(initiateMultipartUploadResult.getUploadId());    }    @Override    public UpLoadPartResult uploadPart(UpLoadCheckPoint upLoadCheckPoint, int partNum, InputStream inputStream) {        UploadPart uploadPart = upLoadCheckPoint.getUploadParts().get(partNum);        long partSize = uploadPart.getSize();        UpLoadPartResult partResult = new UpLoadPartResult(partNum + 1, uploadPart.getOffset(), partSize);        try {            inputStream.skip(uploadPart.getOffset());            UploadPartRequest uploadPartRequest = new UploadPartRequest();            uploadPartRequest.setBucketName(upLoadCheckPoint.getBucket());            uploadPartRequest.setKey(upLoadCheckPoint.getKey());            uploadPartRequest.setUploadId(upLoadCheckPoint.getUploadId());            uploadPartRequest.setInputStream(inputStream);            uploadPartRequest.setPartSize(partSize);            uploadPartRequest.setPartNumber(partNum + 1);            UploadPartResult uploadPartResult = nosClient.uploadPart(uploadPartRequest);            partResult.setNumber(uploadPartResult.getPartNumber());            PartETag eTag = uploadPartResult.getPartETag();            partResult.setEntityTag(new UpLoadPartEntityTag().setETag(eTag.getETag()).setPartNumber(eTag.getPartNumber()));        } catch (Exception e) {            partResult.setFailed(true);            partResult.setException(e);        } finally {            IoUtil.close(inputStream);        }        return partResult;    }    @Override    public void downLoad(OutputStream os, String targetName) {        NOSObject nosObject = nosClient.getObject(getBucket(), getKey(targetName, false));        IoUtil.copy(nosObject.getObjectContent(), os);    }    @Override    public void downLoadCheckPoint(File localFile, String targetName) {        downLoadFile(localFile, targetName, wangYiOssConfig.getSliceConfig(), OssConstant.OssType.WANGYI);    }    @Override    public DownloadObjectStat getDownloadObjectStat(String targetName) {        ObjectMetadata objectMetadata = nosClient.getObjectMetadata(getBucket(), getKey(targetName, false));        DateTime date = DateUtil.date(objectMetadata.getLastModified());        long contentLength = objectMetadata.getContentLength();        String eTag = objectMetadata.getETag();        return new DownloadObjectStat().setSize(contentLength).setLastModified(date).setDigest(eTag);    }    @Override    public void prepareDownload(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) {        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);        downloadCheckPoint.setDownloadFile(localFile.getPath());        downloadCheckPoint.setBucketName(getBucket());        downloadCheckPoint.setKey(getKey(targetName, false));        downloadCheckPoint.setCheckPointFile(checkpointFile);        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));        long downloadSize;        if (downloadCheckPoint.getObjectStat().getSize() > 0) {            Long partSize = wangYiOssConfig.getSliceConfig().getPartSize();            long[] slice = getDownloadSlice(new long[0], downloadCheckPoint.getObjectStat().getSize());            downloadCheckPoint.setDownloadParts(splitDownloadFile(slice[0], slice[1], partSize));            downloadSize = slice[1];        } else {            downloadSize = 0;            downloadCheckPoint.setDownloadParts(splitDownloadOneFile());        }        downloadCheckPoint.setOriginPartSize(downloadCheckPoint.getDownloadParts().size());        createDownloadTemp(downloadCheckPoint.getTempDownloadFile(), downloadSize);    }    @Override    public InputStream downloadPart(String key, long start, long end) {        GetObjectRequest request = new GetObjectRequest(getBucket(), key);        request.setKey(key);        request.setRange(start, end);        NOSObject nosObject = nosClient.getObject(request);        return nosObject.getObjectContent();    }    @Override    public void delete(String targetName) {        nosClient.deleteObject(getBucket(), getKey(targetName, false));    }    @Override    public void copy(String sourceName, String targetName, Boolean isOverride) {        String bucket = getBucket();        String newTargetName = getKey(targetName, false);        if (isOverride || !nosClient.doesObjectExist(bucket, newTargetName, null)) {            nosClient.copyObject(bucket, getKey(sourceName, false), bucket, newTargetName);        }    }    @Override    public OssInfo getInfo(String targetName, Boolean isRecursion) {        String key = getKey(targetName, false);        OssInfo ossInfo = getBaseInfo(key);        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));        if (isRecursion && isDirectory(key)) {            String prefix = convertPath(key, false);            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();            listObjectsRequest.setPrefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);            listObjectsRequest.setBucketName(getBucket());            ObjectListing objectListing = nosClient.listObjects(listObjectsRequest);            List<OssInfo> fileOssInfos = new ArrayList<>();            List<OssInfo> directoryInfos = new ArrayList<>();            if (ObjectUtil.isNotEmpty(objectListing.getObjectSummaries())) {                for (NOSObjectSummary nosObjectSummary : objectListing.getObjectSummaries()) {                    if (FileNameUtil.getName(nosObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {                        ossInfo.setLastUpdateTime(DateUtil.date(nosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));                        ossInfo.setCreateTime(DateUtil.date(nosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));                        ossInfo.setSize(Convert.toStr(nosObjectSummary.getSize()));                    } else {                        fileOssInfos.add(getInfo(replaceKey(nosObjectSummary.getKey(), getBasePath(), false), false));                    }                }            }            if (ObjectUtil.isNotEmpty(objectListing.getCommonPrefixes())) {                for (String commonPrefix : objectListing.getCommonPrefixes()) {                    String target = replaceKey(commonPrefix, getBasePath(), false);                    if (isDirectory(commonPrefix)) {                        directoryInfos.add(getInfo(target, true));                    } else {                        fileOssInfos.add(getInfo(target, false));                    }                }            }            if (ObjectUtil.isNotEmpty(fileOssInfos) && fileOssInfos.get(0) instanceof FileOssInfo) {                ReflectUtil.setFieldValue(ossInfo, "fileInfos", fileOssInfos);            }            if (ObjectUtil.isNotEmpty(directoryInfos) && directoryInfos.get(0) instanceof DirectoryOssInfo) {                ReflectUtil.setFieldValue(ossInfo, "directoryInfos", directoryInfos);            }        }        return ossInfo;    }    @Override    public Boolean isExist(String targetName) {        return nosClient.doesObjectExist(getBucket(), getKey(targetName, false), null);    }    @Override    public String getBasePath() {        return wangYiOssConfig.getBasePath();    }    @Override    public Map<String, Object> getClientObject() {        return new HashMap<String, Object>() {            {                put(NOS_OBJECT_NAME, getNosClient());            }        };    }    private String getBucket() {        return wangYiOssConfig.getBucketName();    }    public OssInfo getBaseInfo(String key) {        OssInfo ossInfo;        if (isFile(key)) {            ossInfo = new FileOssInfo();            try {                ObjectMetadata objectMetadata = nosClient.getObjectMetadata(getBucket(), key);                ossInfo.setLastUpdateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));                ossInfo.setCreateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));                ossInfo.setSize(Convert.toStr(objectMetadata.getContentLength()));            } catch (Exception e) {                log.error("获取{}文件属性失败", key, e);            }        } else {            ossInfo = new DirectoryOssInfo();        }        return ossInfo;    }}