package io.github.artislong.core.minio;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.google.common.io.ByteStreams;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.minio.model.MinioOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.download.DownloadPart;
import io.github.artislong.model.download.DownloadPartResult;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import static com.aliyun.oss.internal.OSSConstants.DEFAULT_BUFFER_SIZE;

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

    private MinioClient minioClient;
    private MinioOssConfig minioOssConfig;

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
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        InputStream inputStream = new FileInputStream(file);
        upLoad(inputStream, targetName, true);
        IoUtil.close(inputStream);
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
    public void downLoadCheckPoint(File localFile, String targetName) {

        String checkpointFile = localFile.getPath() + StrUtil.DOT + OssConstant.OssType.MINIO;

        DownloadCheckPoint downloadCheckPoint = new DownloadCheckPoint();
        try {
            downloadCheckPoint.load(checkpointFile);
        } catch (Exception e) {
            FileUtil.del(checkpointFile);
        }

        DownloadObjectStat downloadObjectStat = getDownloadObjectStat(targetName);
        if (!downloadCheckPoint.isValid(downloadObjectStat)) {
            prepare(downloadCheckPoint, localFile, targetName, checkpointFile);
            FileUtil.del(checkpointFile);
        }

        SliceConfig slice = minioOssConfig.getSliceConfig();

        ExecutorService executorService = Executors.newFixedThreadPool(slice.getTaskNum());
        List<Future<DownloadPartResult>> futures = new ArrayList<>();

        for (int i = 0; i < downloadCheckPoint.getDownloadParts().size(); i++) {
            if (!downloadCheckPoint.getDownloadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new DownloadPartTask(minioClient, downloadCheckPoint, i)));
            }
        }

        executorService.shutdown();

        for (Future<DownloadPartResult> future : futures) {
            try {
                DownloadPartResult partResult = future.get();
                if (partResult.isFailed()) {
                    throw partResult.getException();
                }
            } catch (Exception e) {
                throw new OssException(e);
            }
        }

        try {
            if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new OssException("关闭线程池失败", e);
        }

        FileUtil.rename(new File(downloadCheckPoint.getTempDownloadFile()), downloadCheckPoint.getDownloadFile(), true);
        FileUtil.del(downloadCheckPoint.getCheckPointFile());
    }

    private DownloadObjectStat getDownloadObjectStat(String targetName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        StatObjectArgs statObjectArgs = StatObjectArgs.builder().bucket(getBucket()).object(getKey(targetName, true)).build();
        StatObjectResponse statObjectResponse = minioClient.statObject(statObjectArgs);
        long contentLength = statObjectResponse.size();
        String eTag = statObjectResponse.etag();
        return new DownloadObjectStat().setSize(contentLength)
                .setLastModified(Date.from(statObjectResponse.lastModified().toInstant())).setDigest(eTag);
    }

    private void prepare(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);
        downloadCheckPoint.setDownloadFile(localFile.getPath());
        downloadCheckPoint.setBucketName(getBucket());
        downloadCheckPoint.setObjectKey(getKey(targetName, false));
        downloadCheckPoint.setCheckPointFile(checkpointFile);

        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));

        long downloadSize;
        if (downloadCheckPoint.getObjectStat().getSize() > 0) {
            Long partSize = minioOssConfig.getSliceConfig().getPartSize();
            long[] slice = getSlice(new long[0], downloadCheckPoint.getObjectStat().getSize());
            downloadCheckPoint.setDownloadParts(splitDownloadFile(slice[0], slice[1], partSize));
            downloadSize = slice[1];
        } else {
            //download whole file
            downloadSize = 0;
            downloadCheckPoint.setDownloadParts(splitDownloadOneFile());
        }
        downloadCheckPoint.setOriginPartSize(downloadCheckPoint.getDownloadParts().size());
        downloadCheckPoint.setVersionId(IdUtil.fastSimpleUUID());
        createFixedFile(downloadCheckPoint.getTempDownloadFile(), downloadSize);
    }

    private ArrayList<DownloadPart> splitDownloadFile(long start, long objectSize, long partSize) {
        ArrayList<DownloadPart> parts = new ArrayList<>();

        long partNum = objectSize / partSize;
        if (partNum >= 10000) {
            partSize = objectSize / (10000 - 1);
        }

        long offset = 0L;
        for (int i = 0; offset < objectSize; offset += partSize, i++) {
            DownloadPart part = new DownloadPart();
            part.setIndex(i);
            part.setStart(offset + start);
            part.setEnd(getPartEnd(offset, objectSize, partSize) + start);
            part.setFileStart(offset);
            parts.add(part);
        }

        return parts;
    }

    private long getPartEnd(long begin, long total, long per) {
        if (begin + per > total) {
            return total - 1;
        }
        return begin + per - 1;
    }

    private ArrayList<DownloadPart> splitDownloadOneFile() {
        ArrayList<DownloadPart> parts = new ArrayList<>();
        DownloadPart part = new DownloadPart();
        part.setIndex(0);
        part.setStart(0);
        part.setEnd(-1);
        part.setFileStart(0);
        parts.add(part);
        return parts;
    }

    private long[] getSlice(long[] range, long totalSize) {
        long start = 0;
        long size = totalSize;

        if ((range == null) ||
                (range.length != 2) ||
                (totalSize < 1) ||
                (range[0] < 0 && range[1] < 0) ||
                (range[0] > 0 && range[1] > 0 && range[0] > range[1])||
                (range[0] >= totalSize)) {
            //download all
        } else {
            //dwonload part by range & total size
            long begin = range[0];
            long end = range[1];
            if (range[0] < 0) {
                begin = 0;
            }
            if (range[1] < 0 || range[1] >= totalSize) {
                end = totalSize -1;
            }
            start = begin;
            size = end - begin + 1;
        }

        return new long[]{start, size};
    }

    public static void createFixedFile(String filePath, long length) {
        File file = new File(filePath);
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(file, "rw");
            rf.setLength(length);
        } catch (Exception e) {
            throw new OssException("创建下载缓存文件失败");
        } finally {
            IoUtil.close(rf);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DownloadPartTask implements Callable<DownloadPartResult> {

        MinioClient minioClient;
        DownloadCheckPoint downloadCheckPoint;
        int partNum;

        @Override
        public DownloadPartResult call() {
            DownloadPartResult partResult = null;
            RandomAccessFile output = null;
            InputStream content = null;
            try {
                DownloadPart downloadPart = downloadCheckPoint.getDownloadParts().get(partNum);

                partResult = new DownloadPartResult(partNum + 1, downloadPart.getStart(), downloadPart.getEnd());

                output = new RandomAccessFile(downloadCheckPoint.getTempDownloadFile(), "rw");
                output.seek(downloadPart.getFileStart());

                GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                        .bucket(downloadCheckPoint.getBucketName())
                        .object(downloadCheckPoint.getObjectKey())
                        .offset(downloadPart.getStart()) // 起始字节的位置
                        .length(downloadPart.getEnd())  // 要读取的长度 (可选，如果无值则代表读到文件结尾)。
                        .build();
                content = minioClient.getObject(getObjectArgs);

                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int bytesRead = 0;
                while ((bytesRead = content.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                partResult.setLength(downloadPart.getLength());
                downloadCheckPoint.update(partNum, true);
                downloadCheckPoint.dump(downloadCheckPoint.getCheckPointFile());
            } catch (Exception e) {
                partResult.setException(e);
                partResult.setFailed(true);
            } finally {
                IoUtil.close(output);
                IoUtil.close(content);
            }
            return partResult;
        }
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

    @Override
    public String getBasePath() {
        return minioOssConfig.getBasePath();
    }

    private String getBucket() {
        return minioOssConfig.getBucketName();
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
