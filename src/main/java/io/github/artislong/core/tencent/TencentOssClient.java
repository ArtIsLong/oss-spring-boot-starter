package io.github.artislong.core.tencent;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import io.github.artislong.OssProperties;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * https://cloud.tencent.com/document/product/436
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

    @SneakyThrows
    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        String bucket = getBucket();
        String key = getKey(targetName, false);

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key);

        // 分块上传的过程中，仅能通过初始化分块指定文件上传之后的metadata
        // 需要的头部可以在这里指定
//        ObjectMetadata objectMetadata = new ObjectMetadata();
//        request.setObjectMetadata(objectMetadata);

        InitiateMultipartUploadResult initResult = cosClient.initiateMultipartUpload(request);
        // 获取uploadid
        String uploadId = initResult.getUploadId();

        // 每个分块上传之后都会得到一个返回值 etag，保存起来用于最后合并分块时使用
        List<PartETag> partETags = new LinkedList<>();

        // 上传数据, 这里上传 10 个 1M 的分块数据
        for (int i = 1; i <= 10; i++) {
            // 这里创建一个 ByteArrayInputStream 来作为示例，实际中这里应该是您要上传的 InputStream 类型的流
            InputStream inputStream = new FileInputStream(file);

            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucket);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setUploadId(uploadId);
            uploadPartRequest.setInputStream(inputStream);
            // 设置分块的长度
            uploadPartRequest.setPartSize(1024 * 1024);
            // 设置要上传的分块编号，从 1 开始
            uploadPartRequest.setPartNumber(i);

            UploadPartResult uploadPartResult = cosClient.uploadPart(uploadPartRequest);
            PartETag partETag = uploadPartResult.getPartETag();
            partETags.add(partETag);
        }

        // 分片上传结束后，调用complete完成分片上传
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucket, key, uploadId, partETags);
        cosClient.completeMultipartUpload(completeMultipartUploadRequest);

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
                    ossInfo.setLastUpdateTime(DateUtil.date(cosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setCreateTime(DateUtil.date(cosObjectSummary.getLastModified()).toString(DatePattern.NORM_DATETIME_PATTERN));
                    ossInfo.setSize(Convert.toStr(cosObjectSummary.getSize()));
                } else {
                    fileOssInfos.add(getInfo(replaceKey(cosObjectSummary.getKey(), getBasePath(), false), false));
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
