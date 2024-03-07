package io.github.artislong.core.minio;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.google.common.io.ByteStreams;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.minio.model.MinioOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.utils.OssPathUtil;
import io.minio.*;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * http://docs.minio.org.cn/docs/master/minio-monitoring-guide
 * https://docs.min.io/
 *
 * @author 陈敏
 * @version MinioOssClient.java, v 1.1 2021/11/24 15:35 chenmin Exp $
 * Created on 2021/11/24
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinioOssClient implements StandardOssClient {

    public static final String MINIO_OBJECT_NAME = "minioClient";

    private MinioClient minioClient;
    private MinioOssConfig minioOssConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        try {
            String bucket = getBucket();
            String key = getKey(targetName, true);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(inputStream, inputStream.available(), -1)
                    .build());
        } catch (Exception e) {
            throw new OssException(e);
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        try (InputStream inputStream = FileUtil.getInputStream(file)) {
            upload(inputStream, targetName, true);
        } catch (Exception e) {
            throw new OssException(e);
        }
        return getInfo(targetName);
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {
        GetObjectResponse is = null;
        try {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(getBucket())
                    .object(getKey(targetName, true))
                    .build();
            is = minioClient.getObject(getObjectArgs);
            ByteStreams.copy(is, outputStream);
        } catch (Exception e) {
            throw new OssException(e);
        } finally {
            IoUtil.close(is);
        }
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        downloadfile(localFile, targetName, minioOssConfig.getSliceConfig(), OssType.MINIO);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder().bucket(getBucket()).object(getKey(targetName, true)).build();
            StatObjectResponse statObjectResponse = minioClient.statObject(statObjectArgs);
            long contentLength = statObjectResponse.size();
            String eTag = statObjectResponse.etag();
            return new DownloadObjectStat().setSize(contentLength)
                    .setLastModified(Date.from(statObjectResponse.lastModified().toInstant())).setDigest(eTag);
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
            Long partSize = minioOssConfig.getSliceConfig().getPartSize();
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
    public InputStream downloadPart(String key, long start, long end) throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(getBucket())
                .object(key)
                // 起始字节的位置
                .offset(start)
                // 要读取的长度 (可选，如果无值则代表读到文件结尾)。
                .length(end)
                .build();
        return minioClient.getObject(getObjectArgs);
    }

    @Override
    public void delete(String targetName) {
        try {
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(getBucket())
                    .object(getKey(targetName, true))
                    .build();
            minioClient.removeObject(removeObjectArgs);
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        try {
            CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                    .bucket(getBucket())
                    .object(getKey(targetName, true))
                    .source(CopySource.builder()
                            .bucket(getBucket())
                            .object(getKey(sourceName, true))
                            .build())
                    .build();
            minioClient.copyObject(copyObjectArgs);
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, boolean isRecursion) {
        try {
            String key = getKey(targetName, false);

            OssInfo ossInfo = getBaseInfo(targetName);
            if (isRecursion && isDirectory(key)) {

                String prefix = OssPathUtil.convertPath(key, true);
                ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                        .bucket(getBucket())
                        .delimiter(StrUtil.SLASH)
                        .prefix(prefix.endsWith(StrUtil.SLASH) ? prefix : prefix + StrUtil.SLASH)
                        .build();
                Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);

                List<OssInfo> fileOssInfos = new ArrayList<>();
                List<OssInfo> directoryInfos = new ArrayList<>();

                for (Result<Item> result : results) {
                    Item item = result.get();
                    String childKey = OssPathUtil.replaceKey(item.objectName(), getBasePath(), true);
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
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public String getBasePath() {
        return minioOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(MINIO_OBJECT_NAME, getMinioClient());
            }
        };
    }

    private String getBucket() {
        String bucketName = minioOssConfig.getBucketName();
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bucketName;
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
                ossInfo.setLength(Convert.toStr(headers.get(HttpHeaders.CONTENT_LENGTH)));
            } catch (Exception e) {
                log.error("获取{}文件属性失败", key, e);
            }
        } else {
            ossInfo = new DirectoryOssInfo();
        }
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));
        return ossInfo;
    }

}
