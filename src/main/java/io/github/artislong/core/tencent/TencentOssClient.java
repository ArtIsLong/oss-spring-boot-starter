package io.github.artislong.core.tencent;

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
import io.github.artislong.OssProperties;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import io.github.artislong.exception.NotSupportException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
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
 * @version TencentOssClient.java, v 1.1 2021/11/24 15:35 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TencentOssClient implements StandardOssClient {

    private COSClient cosClient;
    private OssProperties ossProperties;
    private TencentOssProperties tencentOssProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String key = getKey(targetName, false);

        if (isOverride || !cosClient.doesObjectExist(bucketName, key)) {
            cosClient.putObject(bucketName, targetName, is, new ObjectMetadata());
        }
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        COSObject cosObject = cosClient.getObject(getBucket(), getKey(targetName, false));
        IoUtil.copy(cosObject.getObjectContent(), os);
    }

    @Override
    public void delete(String targetName) {
        cosClient.deleteObject(getBucket(), getKey(targetName, false));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String targetKey = getKey(targetName, false);
        if (isOverride || !cosClient.doesObjectExist(bucketName, targetKey)) {
            cosClient.copyObject(getBucket(), getKey(sourceName, false), getBucket(), targetKey);
        }
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        String bucketName = getBucket();
        String targetKey = getKey(targetName, false);
        String sourceKey = getKey(sourceName, false);
        if (isOverride || !cosClient.doesObjectExist(bucketName, targetKey)) {
            cosClient.copyObject(getBucket(), sourceKey, getBucket(), targetKey);
            cosClient.deleteObject(getBucket(), sourceKey);
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
            ObjectListing listObjects = cosClient.listObjects(getBucket(), prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (COSObjectSummary cosObjectSummary : listObjects.getObjectSummaries()) {
                if (FileNameUtil.getName(cosObjectSummary.getKey()).equals(FileNameUtil.getName(key))) {
                    ossInfo.setCreater(cosObjectSummary.getOwner().getDisplayName());
                    ossInfo.setLastUpdateTime(DateUtil.date(cosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(cosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(cosObjectSummary.getSize()));
                } else {
                    OssInfo info = getInfo(replaceKey(cosObjectSummary.getKey(), getBasePath(), false), false);
                    info.setCreater(cosObjectSummary.getOwner().getDisplayName());
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
        return cosClient.doesObjectExist(getBucket(), getKey(targetName, false));
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
        throw new NotSupportException("腾讯云不支持通过SDK创建目录");
    }

    private String getBucket() {
        return tencentOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String key) {
        OssInfo ossInfo;

        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                ObjectMetadata objectMetadata = cosClient.getObjectMetadata(getBucket(), replaceKey(key, "", false));
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
