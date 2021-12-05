package com.github.core.jd;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.github.OssProperties;
import com.github.core.StandardOssClient;
import com.github.core.model.DirectoryOssInfo;
import com.github.core.model.FileOssInfo;
import com.github.core.model.OssInfo;
import com.github.exception.NotSupportException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version JdOssClient.java, v 1.1 2021/11/25 10:44 chenmin Exp $
 * Created on 2021/11/25
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JdOssClient implements StandardOssClient {

    private AmazonS3 amazonS3;
    private TransferManager transferManager;
    private OssProperties ossProperties;
    private JdOssProperties jdOssProperties;

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
    public void downLoad(OutputStream os, String targetName) {
        S3Object s3Object = amazonS3.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(s3Object.getObjectContent(), os);
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
    public void move(String sourceName, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String targetKey = getKey(targetName, false);
        String sourceKey = getKey(sourceName, false);
        if (isOverride || !amazonS3.doesObjectExist(bucketName, targetKey)) {
            amazonS3.copyObject(getBucket(), sourceKey, getBucket(), targetKey);
            amazonS3.deleteObject(getBucket(), sourceKey);
        }
    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {
        move(sourceName, targetName, isOverride);
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            String prefix = convertPath(key, false);
            ObjectListing listObjects = amazonS3.listObjects(getBucket(), prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (S3ObjectSummary s3ObjectSummary : listObjects.getObjectSummaries()) {
                if (FileNameUtil.getName(s3ObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setCreater(s3ObjectSummary.getOwner().getDisplayName());
                    ossInfo.setLastUpdateTime(DateUtil.date(s3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(s3ObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(s3ObjectSummary.getSize()));
                } else {
                    OssInfo info = getInfo(replaceKey(s3ObjectSummary.getKey(), getBasePath(), false), false);
                    info.setCreater(s3ObjectSummary.getOwner().getDisplayName());
                    fileOssInfos.add(info);
                }
            }

            for (String commonPrefix : listObjects.getCommonPrefixes()) {
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
        return amazonS3.doesObjectExist(getBucket(), getKey(targetName, false));
    }

    @Override
    public OssInfo createFile(String targetName) {
        String tempDir = SystemUtil.getUserInfo().getTempDir();
        String localTmpTargetName = getKey(tempDir + targetName, true);
        FileUtil.touch(localTmpTargetName);
        upLoad(FileUtil.getInputStream(localTmpTargetName), targetName);
        FileUtil.del(localTmpTargetName);

        return getInfo(targetName);
    }

    @Override
    public OssInfo createDirectory(String targetName) {
        throw new NotSupportException("京东云不支持通过SDK创建目录");
    }

    private String getBucket() {
        return jdOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = amazonS3.getObjectMetadata(getBucket(), replaceKey(key, "", false));
                ossInfo.setLastUpdateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setCreateTime(DateUtil.date(objectMetadata.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setSize(Convert.toStr(objectMetadata.getContentLength()));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        return ossInfo;
    }

}
