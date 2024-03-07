package io.github.artislong.core.pingan;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.pingan.radosgw.sdk.admin.dto.UserInfo;
import com.pingan.radosgw.sdk.admin.service.RGWAdminServiceFacade;
import com.pingan.radosgw.sdk.service.RadosgwService;
import com.pingan.radosgw.sdk.service.request.ListObjectsRequest;
import com.pingan.radosgw.sdk.service.request.MutilpartUploadFileRequest;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.pingan.model.PingAnOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.utils.OssPathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import repkg.com.amazonaws.AmazonClientException;
import repkg.com.amazonaws.services.s3.model.ObjectListing;
import repkg.com.amazonaws.services.s3.model.ObjectMetadata;
import repkg.com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://yun.pingan.com/ssr/help/storage/obs?menuItem=OBS_SDK_.Java_SDK_
 * @author 陈敏
 * @version PingAnOssClient.java, v 1.1 2022/3/8 10:25 chenmin Exp $
 * Created on 2022/3/8
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PingAnOssClient implements StandardOssClient {

    public static final String RADOSGW_OBJECT_NAME = "radosgwService";

    private PingAnOssConfig pingAnOssConfig;
    private RadosgwService radosgwService;
    RGWAdminServiceFacade rgwAdminServiceFacade;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);

        if (isOverride || !isExist(targetName)) {
            try {
                radosgwService.putObject(bucketName, key, inputStream);
            } catch (AmazonClientException e) {
                throw new OssException(e);
            }
        }
        return getInfo(targetName);
    }

    /**
     * 断点续传，使用SDK断点续传API实现，底层通过分块上传实现
     *
     * @param file       本地文件
     * @param targetName 目标文件路径
     * @return 文件信息
     */
    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        try {
            String bucketName = getBucketName();
            String key = getKey(targetName, false);
            String filePath = file.getPath();
            String checkpointFile = filePath + StrUtil.DOT + OssType.QINGYUN;
            SliceConfig slice = pingAnOssConfig.getSliceConfig();

            MutilpartUploadFileRequest uploadFileRequest = new MutilpartUploadFileRequest();
            uploadFileRequest.setBucket(bucketName);
            uploadFileRequest.setKey(key);
            uploadFileRequest.setUploadFile(filePath);
            uploadFileRequest.setCheckpointFile(checkpointFile);
            uploadFileRequest.setPartSize(slice.getPartSize());
            uploadFileRequest.setEnableCheckpoint(true);

            radosgwService.putObjectMultipart(uploadFileRequest);
        } catch (Throwable e) {
            throw new OssException(e);
        }
        return getInfo(targetName);
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);
        S3Object s3Object = null;
        try {
            s3Object = radosgwService.getObject(bucketName, key);
        } catch (AmazonClientException e) {
            throw new OssException(e);
        }
        IoUtil.copy(s3Object.getObjectContent(), outputStream);
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        log.warn("平安云不支持断点续传下载，将使用普通下载");
        try (OutputStream os = new FileOutputStream(localFile)) {
            download(os, targetName);
        } catch (Exception e) {
            log.error("{}下载失败", targetName, e);
            throw new OssException(e);
        }
    }

    @Override
    public void delete(String targetName) {
        try {
            radosgwService.deleteObject(getBucketName(), getKey(targetName, false));
        } catch (AmazonClientException e) {
            throw new OssException(e);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        String bucketName = getBucketName();
        String targetKey = getKey(targetName, false);
        if (isOverride || !isExist(targetName)) {
//            radosgwService.copyObject(bucketName, getKey(sourceName, false), bucketName, targetKey);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, boolean isRecursion) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(bucketName, key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
            listObjectsRequest.setBucketName(bucketName);
            listObjectsRequest.setDelimiter(StrUtil.SLASH);
            String prefix = OssPathUtil.convertPath(key, false);
            listObjectsRequest.setPrefix(prefix.endsWith(StrUtil.SLASH) ? prefix : prefix + StrUtil.SLASH);
            ObjectListing objectListing = null;
            try {
                objectListing = radosgwService.listObjects(listObjectsRequest);
            } catch (AmazonClientException e) {
                throw new OssException(e);
            }

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (S3Object s3Object : objectListing.getObjectSummaries()) {
                if (FileNameUtil.getName(s3Object.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(s3Object.getCreateTimestamp()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(s3Object.getCreateTimestamp()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setLength(Convert.toStr(s3Object.getObjectMetadata().getContentLength()));
                } else {
                    fileOssInfos.add(getInfo(OssPathUtil.replaceKey(s3Object.getKey(), getBasePath(), false), false));
                }
            }

            for (String commonPrefix : objectListing.getCommonPrefixes()) {
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
        OssInfo info = getInfo(targetName);
        return Convert.toLong(info.getLength()) > 0;
    }

    @Override
    public String getBasePath() {
        return pingAnOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(RADOSGW_OBJECT_NAME, getRadosgwService());
            }
        };
    }

    public String getBucketName() {
        String bucketName = pingAnOssConfig.getBucketName();
        try {
            String userId = pingAnOssConfig.getUserId();
            UserInfo userInfo = rgwAdminServiceFacade.getUser(userId);
            if (ObjectUtil.isEmpty(userInfo)) {
                rgwAdminServiceFacade.createUser(userId, userId);
            }
            if (ObjectUtil.isEmpty(rgwAdminServiceFacade.getBucket(bucketName).getName())) {
                rgwAdminServiceFacade.createBucket(userId, bucketName);
            }
        } catch (AmazonClientException e) {
            log.error("创建Bucket失败", e);
        }
        return bucketName;
    }

    public OssInfo getBaseInfo(String bucketName, String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                S3Object s3Object = radosgwService.getObject(bucketName, OssPathUtil.replaceKey(key, "", false));
                ObjectMetadata objectMetadata = s3Object.getObjectMetadata();
                ossInfo.setLastUpdateTime(DateUtil.parse(objectMetadata.getMetadata().get(HttpHeaders.LAST_MODIFIED)).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.parse(objectMetadata.getMetadata().get(HttpHeaders.DATE)).toString(DatePattern.NORM_DATETIME_PATTERN));
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
