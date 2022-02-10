package io.github.artislong.core.ali;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.model.*;
import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * https://help.aliyun.com/product/31815.html
 *
 * @author 陈敏
 * @version AliOssClient.java, v 1.1 2021/11/15 11:12 chenmin Exp $
 * Created on 2021/11/15
 */
@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class AliOssClient implements StandardOssClient {

    private OSS oss;
    private OssProperties ossProperties;
    private AliOssProperties aliOssProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);

        if (isOverride || !oss.doesObjectExist(bucketName, key)) {
            oss.putObject(bucketName, key, is, new ObjectMetadata());
        }
        OssInfo ossInfo = getBaseInfo(bucketName, key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));

        return ossInfo;
    }

    @SneakyThrows
    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);

        UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName,key);
        uploadFileRequest.setUploadFile(file.getPath());

        AliOssProperties.Slice slice = getAliOssProperties().getSlice();
        if (ObjectUtil.isNotEmpty(slice)) {
            uploadFileRequest.setTaskNum(slice.getTaskNum());
            uploadFileRequest.setPartSize(slice.getPartSize());
        }

        uploadFileRequest.setEnableCheckpoint(true);

        String checkpointFile = file.getPath() + StrUtil.DOT + OssConstant.OssType.ALI;
        uploadFileRequest.setCheckpointFile(checkpointFile);

        oss.uploadFile(uploadFileRequest);
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);
        OSSObject ossObject = oss.getObject(bucketName, key);
        IoUtil.copy(ossObject.getObjectContent(), os);
        IoUtil.close(ossObject);
    }

    @Override
    public void delete(String targetName) {
        oss.deleteObject(getBucketName(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucketName = getBucketName();
        String targetKey = getKey(targetName, false);
        if (isOverride || !oss.doesObjectExist(bucketName, targetKey)) {
            oss.copyObject(bucketName, getKey(sourceName, false), bucketName, targetKey);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String bucketName = getBucketName();
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(bucketName, key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setDelimiter("/");
            String prefix = convertPath(key, false);
            listObjectsRequest.setPrefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);
            ObjectListing listing = oss.listObjects(listObjectsRequest);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (OSSObjectSummary ossObjectSummary : listing.getObjectSummaries()) {
                if (FileNameUtil.getName(ossObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(ossObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(ossObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(ossObjectSummary.getSize()));
                } else {
                    fileOssInfos.add(getInfo(replaceKey(ossObjectSummary.getKey(), getBasePath(), false), false));
                }
            }

            for (String commonPrefix : listing.getCommonPrefixes()) {
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
        return oss.doesObjectExist(getBucketName(), getKey(targetName, false));
    }

    public String getBucketName() {
        return aliOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String bucketName, String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = oss.getObjectMetadata(bucketName, replaceKey(key, "", false));
                ossInfo.setLastUpdateTime(DateUtil.date((Date) objectMetadata.getRawMetadata().get(HttpHeaders.LAST_MODIFIED)).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.date((Date) objectMetadata.getRawMetadata().get(HttpHeaders.DATE)).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setSize(Convert.toStr(objectMetadata.getContentLength()));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        return ossInfo;
    }

    private OssInfo getDirectoryBaseInfo(String bucketName, String key) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
        listObjectsRequest.setDelimiter("/");
        String prefix = convertPath(key, false);
        listObjectsRequest.setPrefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);
        ObjectListing listing = oss.listObjects(listObjectsRequest);

        OssInfo ossInfo = new DirectoryOssInfo();
        for (OSSObjectSummary ossObjectSummary : listing.getObjectSummaries()) {
            if (FileNameUtil.getName(ossObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                ossInfo.setLastUpdateTime(DateUtil.date(ossObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.date(ossObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setSize(Convert.toStr(ossObjectSummary.getSize()));
            }
        }
        return ossInfo;
    }

}
