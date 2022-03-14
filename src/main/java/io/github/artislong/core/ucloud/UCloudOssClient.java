package io.github.artislong.core.ucloud;

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
import cn.ucloud.ufile.UfileClient;
import cn.ucloud.ufile.api.object.ObjectApiBuilder;
import cn.ucloud.ufile.api.object.multi.MultiUploadInfo;
import cn.ucloud.ufile.api.object.multi.MultiUploadPartState;
import cn.ucloud.ufile.bean.DownloadFileBean;
import cn.ucloud.ufile.bean.ObjectInfoBean;
import cn.ucloud.ufile.bean.ObjectListBean;
import cn.ucloud.ufile.bean.ObjectProfile;
import cn.ucloud.ufile.exception.UfileClientException;
import cn.ucloud.ufile.util.StorageType;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.ucloud.model.UCloudOssConfig;
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
 * https://docs.ucloud.cn/ufile/README
 * @author 陈敏
 * @version UCloudOssClient.java, v 1.1 2022/3/7 0:20 chenmin Exp $
 * Created on 2022/3/7
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UCloudOssClient implements StandardOssClient {

    public static final String OBJECT_OBJECT_NAME = "objectApiBuilder";

    private UfileClient ufileClient;
    private ObjectApiBuilder objectApiBuilder;
    private UCloudOssConfig uCloudOssConfig;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String key = getKey(targetName, false);

        if (isOverride || !isExist(targetName)) {
            try {
                objectApiBuilder.putObject(is, 0, "")
                        .nameAs(key)
                        .toBucket(bucketName)
                        .execute();
            } catch (Exception e) {
                throw new OssException(e);
            }
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        uploadFile(file, targetName, uCloudOssConfig.getSliceConfig(), OssConstant.OssType.UCLOUD);
        return getInfo(targetName);
    }

    @Override
    public void completeUpload(UpLoadCheckPoint upLoadCheckPoint, List<UpLoadPartEntityTag> partEntityTags) {
        List<MultiUploadPartState> partStates = partEntityTags.stream().sorted(Comparator.comparingInt(UpLoadPartEntityTag::getPartNumber))
                .map(partEntityTag -> {
                    MultiUploadPartState multiUploadPartState = new MultiUploadPartState();
                    // TODO
                    return multiUploadPartState;
                }).collect(Collectors.toList());

        MultiUploadInfo multiUploadInfo = new MultiUploadInfo();
        objectApiBuilder.finishMultiUpload(multiUploadInfo, partStates);

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
            MultiUploadInfo multiUploadInfo = objectApiBuilder.initMultiUpload(key, "", bucket).withStorageType(StorageType.STANDARD).execute();
            uploadCheckPoint.setUploadId(multiUploadInfo.getUploadId());
        } catch (Exception e) {
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

            MultiUploadInfo multiUploadInfo = new MultiUploadInfo();
            MultiUploadPartState multiUploadPartState = objectApiBuilder.multiUploadPart(multiUploadInfo, null, partNum)
                    .from(null, Convert.toInt(uploadPart.getOffset()), Convert.toInt(partSize), partNum).execute();

            partResult.setNumber(multiUploadPartState.getPartIndex());
            partResult.setEntityTag(new UpLoadPartEntityTag().setETag(multiUploadPartState.geteTag())
                    .setPartNumber(multiUploadPartState.getPartIndex()));
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
        ObjectProfile objectProfile = new ObjectProfile();
        objectProfile.setBucket(getBucket());
        objectProfile.setKeyName(getKey(targetName, false));
        try {
            DownloadFileBean downloadFileBean = objectApiBuilder.downloadFile(objectProfile).execute();
            IoUtil.copy(FileUtil.getInputStream(downloadFileBean.getFile()), os);
        } catch (UfileClientException e) {
            throw new OssException(e);
        }
    }

    @Override
    public void downLoadCheckPoint(File localFile, String targetName) {
        downLoadFile(localFile, targetName, uCloudOssConfig.getSliceConfig(), OssConstant.OssType.TENCENT);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        try {
            ObjectProfile objectProfile = objectApiBuilder.objectProfile(getKey(targetName, false), getBucket()).execute();
            DateTime date = DateUtil.parse(objectProfile.getLastModified());
            long contentLength = objectProfile.getContentLength();
            String eTag = objectProfile.geteTag();
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
            Long partSize = uCloudOssConfig.getSliceConfig().getPartSize();
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
        ObjectProfile objectProfile = new ObjectProfile();
        objectProfile.setBucket(getBucket());
        objectProfile.setKeyName(key);
        try {
            DownloadFileBean downloadFileBean = objectApiBuilder.downloadFile(objectProfile).withinRange(start, end).execute();
            return FileUtil.getInputStream(downloadFileBean.getFile());
        } catch (UfileClientException e) {
            throw new OssException(e);
        }
    }

    @Override
    public void delete(String targetName) {
        objectApiBuilder.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String targetKey = getKey(targetName, false);
        if (isOverride || !isExist(targetName)) {
            try {
                objectApiBuilder.copyObject(bucketName, getKey(sourceName, false)).copyTo(bucketName, targetKey).execute();
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
            String prefix = OssPathUtil.convertPath(key, false);
            ObjectListBean objectListBean;
            try {
                objectListBean = objectApiBuilder.objectList(getBucket())
                        .withPrefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH)
                        .execute();
            } catch (Exception e) {
                throw new OssException(e);
            }

            List<ObjectInfoBean> objectList = objectListBean.getObjectList();

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (ObjectInfoBean objectInfoBean : objectList) {
                String fileName = objectInfoBean.getFileName();
                if (FileNameUtil.getName(fileName).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(objectInfoBean.getModifyTime()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(objectInfoBean.getCreateTime()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setLength(Convert.toStr(objectInfoBean.getSize()));
                } else if (isDirectory(fileName)) {
                    directoryInfos.add(getInfo(OssPathUtil.replaceKey(fileName, getBasePath(), false), true));
                } else {
                    fileOssInfos.add(getInfo(OssPathUtil.replaceKey(fileName, getBasePath(), false), false));
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
        try {
            ObjectProfile objectProfile = objectApiBuilder.objectProfile(getKey(targetName, false), getBucket()).execute();
            return objectProfile.getContentLength() > 0;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    @Override
    public String getBasePath() {
        return uCloudOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(OBJECT_OBJECT_NAME, getObjectApiBuilder());
            }
        };
    }

    private String getBucket() {
        return uCloudOssConfig.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectProfile objectProfile = getObjectApiBuilder().objectProfile(OssPathUtil.replaceKey(key, getBasePath(), false), getBucket()).execute();
                ossInfo.setLastUpdateTime(DateUtil.parse(objectProfile.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.parse(objectProfile.getCreateTime()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setLength(Convert.toStr(objectProfile.getContentLength()));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        return ossInfo;
    }

}
