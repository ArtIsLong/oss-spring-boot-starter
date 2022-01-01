package io.github.artislong.core.minio;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.google.common.io.ByteStreams;
import io.github.artislong.OssProperties;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import io.minio.*;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version MinioOssClient.java, v 1.1 2021/11/24 15:35 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinioOssClient implements StandardOssClient {

    private MinioClient minioClient;
    private OssProperties ossProperties;
    private MinioOssProperties minioOssProperties;

    @SneakyThrows
    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String bucket = getBucket();
        String key = getKey(targetName, true);
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .stream(is, is.available(), -1)
                .build());
        return getInfo(targetName);
    }

    @SneakyThrows
    @Override
    public void downLoad(OutputStream os, String targetName) {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(getBucket())
                .object(getKey(targetName, true))
                .build();
        GetObjectResponse is = minioClient.getObject(getObjectArgs);
        ByteStreams.copy(is, os);
        IoUtil.close(is);
    }

    @SneakyThrows
    @Override
    public void delete(String targetName) {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(getBucket())
                .object(getKey(targetName, true))
                .build();
        minioClient.removeObject(removeObjectArgs);
    }

    @SneakyThrows
    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                .bucket(getBucket())
                .object(getKey(targetName, true))
                .source(CopySource.builder()
                        .bucket(getBucket())
                        .object(getKey(sourceName, true))
                        .build())
                .build();
        minioClient.copyObject(copyObjectArgs);
    }

    @SneakyThrows
    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, false);

        OssInfo ossInfo = getBaseInfo(targetName);
        if (isRecursion && isDirectory(key)) {

            String prefix = convertPath(key, true);
            ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                    .bucket(getBucket())
                    .delimiter("/")
                    .prefix(prefix.endsWith("/") ? prefix : prefix + CharPool.SLASH)
                    .build();
            Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);

            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                String childKey = replaceKey(item.objectName(), getBasePath(), true);
                if (item.isDir()) {
                    directoryInfos.add(getInfo(childKey, true));
                } else {
                    fileOssInfos.add(getInfo(childKey, false));
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

    private String getBucket() {
        return minioOssProperties.getBucketName();
    }

    public OssInfo getBaseInfo(String targetName) {
        String key = getKey(targetName, true);
        OssInfo ossInfo;
        String bucketName = getBucket();
        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            try {
                GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucketName).object(key).build();
                GetObjectResponse objectResponse = minioClient.getObject(getObjectArgs);
                Headers headers = objectResponse.headers();

                ossInfo.setCreateTime(DateUtil.date(headers.getDate(HttpHeaders.DATE)).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setLastUpdateTime(DateUtil.date(headers.getDate(HttpHeaders.LAST_MODIFIED)).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setSize(Convert.toStr(headers.get(HttpHeaders.CONTENT_LENGTH)));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, ossInfo.getName(), true));
        return ossInfo;
    }

}
