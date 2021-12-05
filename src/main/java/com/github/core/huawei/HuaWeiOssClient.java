package com.github.core.huawei;

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
import com.github.OssProperties;
import com.github.core.StandardOssClient;
import com.github.core.model.DirectoryOssInfo;
import com.github.core.model.FileOssInfo;
import com.github.core.model.OssInfo;
import com.github.exception.NotSupportException;
import com.obs.services.ObsClient;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
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
 * @version HuaWeiOssClient.java, v 1.1 2021/11/25 10:01 chenmin Exp $
 * Created on 2021/11/25
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HuaWeiOssClient implements StandardOssClient {

    private ObsClient obsClient;
    private HuaWeiOssProperties huaWeiOssProperties;
    private OssProperties ossProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !obsClient.doesObjectExist(bucket, key)) {
            obsClient.putObject(bucket, key, is);
        }
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        ObsObject obsObject = obsClient.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(obsObject.getObjectContent(), os);
    }

    @Override
    public void delete(String targetName) {
        obsClient.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String newTargetName = getKey(targetName, false);
        if (isOverride || !obsClient.doesObjectExist(bucket, newTargetName)) {
            obsClient.copyObject(bucket, getKey(sourceName, false), bucket, newTargetName);
        }
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        String newTargetName = getKey(targetName, false);
        String bucket = getBucket();
        if (isOverride || !obsClient.doesObjectExist(bucket, newTargetName)) {
            obsClient.copyObject(bucket, getKey(sourceName, false), bucket, newTargetName);
            obsClient.deleteObject(bucket, newTargetName);
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
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(getBucket());
            listObjectsRequest.setDelimiter("/");
            String prefix = convertPath(key, false);
            listObjectsRequest.setPrefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);

            ObjectListing listObjects = obsClient.listObjects(listObjectsRequest);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (ObsObject obsObject : listObjects.getObjects()) {
                if (FileNameUtil.getName(obsObject.getObjectKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setCreater(obsObject.getOwner().getDisplayName());
                    ossInfo.setLastUpdateTime(DateUtil.date(obsObject.getMetadata().getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(obsObject.getMetadata().getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(obsObject.getMetadata().getContentLength()));
                } else {
                    OssInfo info = getInfo(replaceKey(obsObject.getObjectKey(), getBasePath(), false), false);
                    info.setCreater(obsObject.getOwner().getDisplayName());
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
        return obsClient.doesObjectExist(getBucket(), getKey(targetName, false));
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
        throw new NotSupportException("华为云不支持通过SDK创建目录");
    }

    private String getBucket() {
        return huaWeiOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = obsClient.getObjectMetadata(getBucket(), key);
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
