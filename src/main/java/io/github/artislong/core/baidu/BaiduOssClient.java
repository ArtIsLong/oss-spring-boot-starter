package io.github.artislong.core.baidu;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.BosObject;
import com.baidubce.services.bos.model.BosObjectSummary;
import com.baidubce.services.bos.model.ListObjectsResponse;
import com.baidubce.services.bos.model.ObjectMetadata;
import io.github.artislong.OssProperties;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import io.github.artislong.exception.NotSupportException;
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
 * @version BaiduOssClient.java, v 1.1 2021/11/24 15:34 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaiduOssClient implements StandardOssClient {

    private BosClient bosClient;
    private OssProperties ossProperties;
    private BaiduOssProperties baiduOssProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, false);
        if (isOverride || !bosClient.doesObjectExist(bucket, key)) {
            bosClient.putObject(bucket, targetName, is);
        }
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        BosObject bosObject = bosClient.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(bosObject.getObjectContent(), os);
    }

    @Override
    public void delete(String targetName) {
        bosClient.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String newTargetName = getKey(targetName, false);
        if (isOverride || !bosClient.doesObjectExist(bucket, newTargetName)) {
            bosClient.copyObject(bucket, getKey(sourceName, false), bucket, newTargetName);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(key);
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));

        if (isRecursion && isDirectory(key)) {
            String prefix = convertPath(key, false);
            ListObjectsResponse listObjects = bosClient.listObjects(getBucket(), prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (BosObjectSummary bosObjectSummary : listObjects.getContents()) {
                if (FileNameUtil.getName(bosObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setLastUpdateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(bosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(bosObjectSummary.getSize()));
                } else {
                    fileOssInfos.add(getInfo(replaceKey(bosObjectSummary.getKey(), getBasePath(), false), false));
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
        return bosClient.doesObjectExist(getBucket(), getKey(targetName, false));
    }

    @Override
    public OssInfo createDirectory(String targetName) {
        throw new NotSupportException("百度云不支持通过SDK创建目录");
    }

    private String getBucket() {
        return baiduOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = bosClient.getObjectMetadata(getBucket(), key);
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
