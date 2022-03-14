package io.github.artislong.core.qingyun;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baidubce.services.bos.model.PartETag;
import com.qingstor.sdk.exception.QSException;
import com.qingstor.sdk.service.Bucket;
import com.qingstor.sdk.service.QingStor;
import com.qingstor.sdk.service.Types;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.qingyun.model.QingYunOssConfig;
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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 陈敏
 * @version QingYunOssClient.java, v 1.0 2022/3/10 23:52 chenmin Exp $
 * Created on 2022/3/10
 */
@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class QingYunOssClient implements StandardOssClient {

    public static final String QING_OBJECT_NAME = "oss";

    private QingStor qingStor;
    private Bucket bucketClient;
    private QingYunOssConfig qingYunOssConfig;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !isExist(targetName)) {
            Bucket.PutObjectInput input = new Bucket.PutObjectInput();
            input.setBodyInputStream(is);
            try {
                bucketClient.putObject(bucket, input);
            } catch (Exception e) {
                throw new OssException(e);
            }
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        return uploadFile(file, targetName, qingYunOssConfig.getSliceConfig(), OssConstant.OssType.BAIDU);
    }

    @Override
    public void completeUpload(UpLoadCheckPoint upLoadCheckPoint, List<UpLoadPartEntityTag> partEntityTags) {
        List<PartETag> eTags = partEntityTags.stream().sorted(Comparator.comparingInt(UpLoadPartEntityTag::getPartNumber))
                .map(partEntityTag -> {
                    PartETag p = new PartETag();
                    p.setETag(partEntityTag.getETag());
                    p.setPartNumber(partEntityTag.getPartNumber());
                    return p;
                }).collect(Collectors.toList());

        Bucket.CompleteMultipartUploadInput input = new Bucket.CompleteMultipartUploadInput();
        input.setUploadID(upLoadCheckPoint.getUploadId());
        try {
            bucketClient.completeMultipartUpload(upLoadCheckPoint.getKey(), input);
        } catch (QSException e) {
            throw new OssException(e);
        }
        FileUtil.del(upLoadCheckPoint.getCheckpointFile());
    }

    @Override
    public void prepareUpload(UpLoadCheckPoint uploadCheckPoint, File upLoadFile, String targetName, String checkpointFile, SliceConfig slice) {
        String bucket = getBucket();
        String key = getKey(targetName, false);

        uploadCheckPoint.setMagic(UpLoadCheckPoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(upLoadFile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setBucket(bucket);
        uploadCheckPoint.setCheckpointFile(checkpointFile);
        uploadCheckPoint.setUploadFileStat(UpLoadFileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = slice.getPartSize();
        long fileLength = upLoadFile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitUploadFile(uploadCheckPoint.getUploadFileStat().getSize(), partSize));
        uploadCheckPoint.setPartEntityTags(new ArrayList<>());
        uploadCheckPoint.setOriginPartSize(parts);

        try {
            Bucket.InitiateMultipartUploadInput input = new Bucket.InitiateMultipartUploadInput();
            Bucket.InitiateMultipartUploadOutput multipartUploadOutput = bucketClient.initiateMultipartUpload(key, input);
            uploadCheckPoint.setUploadId(multipartUploadOutput.getUploadID());
        } catch (QSException e) {
            throw new OssException(e);
        }

    }

    @Override
    public UpLoadPartResult uploadPart(UpLoadCheckPoint upLoadCheckPoint, int partNum, InputStream inputStream) {
        UploadPart uploadPart = upLoadCheckPoint.getUploadParts().get(partNum);
        long partSize = uploadPart.getSize();
        UpLoadPartResult partResult = new UpLoadPartResult(partNum + 1, uploadPart.getOffset(), partSize);

        try {
            inputStream.skip(uploadPart.getOffset());

            Bucket.UploadMultipartInput input = new Bucket.UploadMultipartInput();
            input.setPartNumber(partNum + 1);
            input.setFileOffset(uploadPart.getOffset());
            input.setUploadID(upLoadCheckPoint.getUploadId());
            input.setBodyInputStream(inputStream);
            Bucket.UploadMultipartOutput multipartOutput = bucketClient.uploadMultipart(upLoadCheckPoint.getKey(), input);

            partResult.setNumber(partNum + 1);
            partResult.setEntityTag(new UpLoadPartEntityTag().setETag(multipartOutput.getETag()).setPartNumber(partNum));
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
        try {
            Bucket.GetObjectInput input = new Bucket.GetObjectInput();
            Bucket.GetObjectOutput object = bucketClient.getObject(getKey(targetName, false), input);
            IoUtil.copy(object.getBodyInputStream(), os);
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public void downLoadCheckPoint(File localFile, String targetName) {
        downLoadFile(localFile, targetName, qingYunOssConfig.getSliceConfig(), OssConstant.OssType.BAIDU);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        try {
            Bucket.GetObjectInput input = new Bucket.GetObjectInput();
            Bucket.GetObjectOutput object = bucketClient.getObject(getKey(targetName, false), input);
            DateTime date = DateUtil.parse(object.getLastModified());
            long contentLength = object.getContentLength();
            String eTag = object.getETag();
            return new DownloadObjectStat().setSize(contentLength).setLastModified(date).setDigest(eTag);
        } catch (Exception e) {
            throw new OssException(e);
        }
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
            Long partSize = qingYunOssConfig.getSliceConfig().getPartSize();
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
        try {
            Bucket.GetObjectInput putObjectInput = new Bucket.GetObjectInput();
            putObjectInput.setRange("bytes=" + start + "-" + end);
            Bucket.GetObjectOutput object = bucketClient.getObject(key, putObjectInput);
            return object.getBodyInputStream();
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public void delete(String targetName) {
        try {
            bucketClient.deleteObject(getKey(targetName, false));
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String newSourceName = getKey(sourceName, false);
        String newTargetName = getKey(targetName, false);
        if (isOverride || !isExist(targetName)) {
            try {
                Bucket.PutObjectInput input = new Bucket.PutObjectInput();
                input.setXQSCopySource("/" + bucket + "/" + newSourceName);
                bucketClient.putObject(newTargetName, input);
            } catch (Exception e) {
                throw new OssException(e);
            }
        }
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String newSourceName = getKey(sourceName, false);
        String newTargetName = getKey(targetName, false);
        if (isOverride || !isExist(targetName)) {
            try {
                Bucket.PutObjectInput input = new Bucket.PutObjectInput();
                input.setXQSMoveSource("/" + bucket + "/" + newSourceName);
                bucketClient.putObject(newTargetName, input);
            } catch (Exception e) {
                throw new OssException(e);
            }
        }
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();

            try {
                String prefix = OssPathUtil.convertPath(key, false);
                Bucket.ListObjectsInput input = new Bucket.ListObjectsInput();
                input.setPrefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);
                Bucket.ListObjectsOutput listObjects = bucketClient.listObjects(input);

                if (ObjectUtil.isNotEmpty(listObjects.getKeys())) {
                    for (Types.KeyModel keyModel : listObjects.getKeys()) {
                        if (FileNameUtil.getName(keyModel.getKey()).equals(FileNameUtil.getName(key))) {
                            ossInfo.setLastUpdateTime(DateUtil.parse(keyModel.getCreated()).toString(DatePattern.NORM_DATETIME_PATTERN));
                            ossInfo.setCreateTime(DateUtil.parse(keyModel.getCreated()).toString(DatePattern.NORM_DATETIME_PATTERN));
                            ossInfo.setLength(Convert.toStr(keyModel.getSize()));
                        } else {
                            fileOssInfos.add(getInfo(OssPathUtil.replaceKey(keyModel.getKey(), getBasePath(), false), false));
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
            } catch (Exception e) {
                throw new OssException(e);
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
        OssInfo info = getInfo(targetName);
        return Convert.toLong(info.getLength()) > 0;
    }

    @Override
    public String getBasePath() {
        return qingYunOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(QING_OBJECT_NAME, getQingStor());
            }
        };
    }

    private String getBucket() {
        return qingYunOssConfig.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                Bucket.GetObjectOutput object = bucketClient.getObject(key, new Bucket.GetObjectInput());
                ossInfo.setLastUpdateTime(DateUtil.parse(object.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.parse(object.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setLength(Convert.toStr(object.getContentLength()));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        return ossInfo;
    }

}
