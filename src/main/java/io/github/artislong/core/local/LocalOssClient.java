package io.github.artislong.core.local;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.local.model.LocalOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.download.DownloadPart;
import io.github.artislong.model.download.DownloadPartResult;
import io.github.artislong.model.upload.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 本地文件操作客户端
 *
 * @author 陈敏
 * @version LocalOssClient.java, v 1.1 2021/11/5 15:44 chenmin Exp $
 * Created on 2021/11/5
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalOssClient implements StandardOssClient {

    private LocalOssConfig localOssConfig;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String key = getKey(targetName, true);
        if (isOverride && FileUtil.exist(key)) {
            FileUtil.del(key);
        }
        File file = FileUtil.writeFromStream(is, key);

        OssInfo ossInfo = getBaseInfo(file.getPath());

        ossInfo.setName(file.getName());
        ossInfo.setPath(replaceKey(targetName, file.getName(), true));
        return ossInfo;
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        upLoadFile(file, targetName);
        return getInfo(targetName);
    }

    public void upLoadFile(File upLoadFile, String targetName) {
        String checkpointFile = upLoadFile.getPath() + StrUtil.DOT + OssConstant.OssType.LOCAL;

        UpLoadCheckPoint upLoadCheckPoint = new UpLoadCheckPoint();
        try {
            upLoadCheckPoint.load(checkpointFile);
        } catch (Exception e) {
            FileUtil.del(checkpointFile);
        }

        if (!upLoadCheckPoint.isValid()) {
            prepareUpload(upLoadCheckPoint, upLoadFile, targetName, checkpointFile);
            FileUtil.del(checkpointFile);
        }

        Integer taskNum = localOssConfig.getSliceConfig().getTaskNum();

        ExecutorService executorService = Executors.newFixedThreadPool(taskNum);
        List<Future<UpLoadPartResult>> futures = new ArrayList<>();

        for (int i = 0; i < upLoadCheckPoint.getUploadParts().size(); i++) {
            if (!upLoadCheckPoint.getUploadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new UploadPartTask(upLoadCheckPoint, i)));
            }
        }

        executorService.shutdown();

        for (int i = 0; i < futures.size(); i++) {
            Future<UpLoadPartResult> future = futures.get(i);
            try {
                UpLoadPartResult partResult = future.get();
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

        FileUtil.del(checkpointFile);
    }

    private void prepareUpload(UpLoadCheckPoint uploadCheckPoint, File upLoadFile, String targetName, String checkpointFile) {
        String key = getKey(targetName, true);

        uploadCheckPoint.setMagic(UpLoadCheckPoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(upLoadFile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setCheckpointFile(checkpointFile);
        uploadCheckPoint.setUploadFileStat(UpLoadFileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = localOssConfig.getSliceConfig().getPartSize();
        long fileLength = upLoadFile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitUploadFile(uploadCheckPoint.getUploadFileStat().getSize(), parts));
        uploadCheckPoint.setOriginPartSize(parts);
    }

    private ArrayList<UploadPart> splitUploadFile(long fileSize, long partSize) {
        ArrayList<UploadPart> parts = new ArrayList<>();

        long partNum = fileSize / partSize;
        if (partNum >= OssConstant.DEFAULT_PART_NUM) {
            partSize = fileSize / (OssConstant.DEFAULT_PART_NUM - 1);
            partNum = fileSize / partSize;
        }

        for (long i = 0; i < partNum; i++) {
            UploadPart part = new UploadPart();
            part.setNumber((int) (i + 1));
            part.setOffset(i * partSize);
            part.setSize(partSize);
            part.setCompleted(false);
            parts.add(part);
        }

        if (fileSize % partSize > 0) {
            UploadPart part = new UploadPart();
            part.setNumber(parts.size() + 1);
            part.setOffset(parts.size() * partSize);
            part.setSize(fileSize % partSize);
            part.setCompleted(false);
            parts.add(part);
        }

        return parts;
    }

    @Slf4j
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadPartTask implements Callable<UpLoadPartResult> {
        private UpLoadCheckPoint upLoadCheckPoint;
        private int partNum;

        @Override
        public UpLoadPartResult call() {
            UpLoadPartResult partResult = null;
            try {
                UploadPart uploadPart = upLoadCheckPoint.getUploadParts().get(partNum);
                long offset = uploadPart.getOffset();
                long size = uploadPart.getSize();
                Integer partOffset = Convert.toInt(offset);
                Integer partSize = Convert.toInt(size);

                partResult = new UpLoadPartResult(partNum + 1, offset, size);
                partResult.setNumber(partNum);

                RandomAccessFile uploadFile = new RandomAccessFile(upLoadCheckPoint.getUploadFile(), "r");
                RandomAccessFile targetFile = new RandomAccessFile(upLoadCheckPoint.getKey(), "rw");

                byte[] data = new byte[partSize];
                uploadFile.seek(offset);
                targetFile.seek(offset);
                int len = uploadFile.read(data);
                log.info("partNum = {}, partOffset = {}, partSize = {}", partNum, partOffset, partSize);
                targetFile.write(data, 0, len);

                upLoadCheckPoint.update(partNum, new UpLoadPartEntityTag(), true);
                upLoadCheckPoint.dump();
            } catch (Exception e) {
                partResult.setFailed(true);
                partResult.setException(e);
            }

            return partResult;
        }
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        FileUtil.writeToStream(getKey(targetName, true), os);
    }

    @Override
    public void downLoadCheckPoint(File localFile, String targetName) {

        String checkpointFile = localFile.getPath() + StrUtil.DOT + OssConstant.OssType.BAIDU;

        DownloadCheckPoint downloadCheckPoint = new DownloadCheckPoint();
        try {
            downloadCheckPoint.load(checkpointFile);
        } catch (Exception e) {
            FileUtil.del(checkpointFile);
        }

        DownloadObjectStat downloadObjectStat = getDownloadObjectStat(targetName);
        if (!downloadCheckPoint.isValid(downloadObjectStat)) {
            prepareDownload(downloadCheckPoint, localFile, targetName, checkpointFile);
            FileUtil.del(checkpointFile);
        }

        SliceConfig slice = localOssConfig.getSliceConfig();

        ExecutorService executorService = Executors.newFixedThreadPool(slice.getTaskNum());
        List<Future<DownloadPartResult>> futures = new ArrayList<>();

        for (int i = 0; i < downloadCheckPoint.getDownloadParts().size(); i++) {
            if (!downloadCheckPoint.getDownloadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new DownloadPartTask(downloadCheckPoint, i)));
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

    private DownloadObjectStat getDownloadObjectStat(String targetName) {
        File file = new File(getKey(targetName, true));
        return new DownloadObjectStat()
                .setSize(file.length())
                .setLastModified(new Date(file.lastModified()))
                .setDigest(DigestUtil.sha256Hex(Convert.toStr(file.lastModified())));
    }

    private void prepareDownload(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) {
        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);
        downloadCheckPoint.setDownloadFile(localFile.getPath());
        downloadCheckPoint.setKey(getKey(targetName, false));
        downloadCheckPoint.setCheckPointFile(checkpointFile);

        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));

        long downloadSize;
        if (downloadCheckPoint.getObjectStat().getSize() > 0) {
            Long partSize = localOssConfig.getSliceConfig().getPartSize();
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
        if (partNum >= OssConstant.DEFAULT_PART_NUM) {
            partSize = objectSize / (OssConstant.DEFAULT_PART_NUM - 1);
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

    @Slf4j
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DownloadPartTask implements Callable<DownloadPartResult> {

        DownloadCheckPoint downloadCheckPoint;
        int partNum;

        @Override
        public DownloadPartResult call() {
            DownloadPartResult downloadPartResult = null;
            try {
                DownloadPart downloadPart = downloadCheckPoint.getDownloadParts().get(partNum);
                long start = downloadPart.getStart();
                long end = downloadPart.getEnd();
                Integer partOffset = Convert.toInt(start);
                Integer partSize = Convert.toInt(end);

                downloadPartResult = new DownloadPartResult(partNum + 1, start, end);
                downloadPartResult.setNumber(partNum);

                RandomAccessFile uploadFile = new RandomAccessFile(downloadCheckPoint.getDownloadFile(), "r");
                RandomAccessFile targetFile = new RandomAccessFile(downloadCheckPoint.getKey(), "rw");

                byte[] data = new byte[partSize];
                uploadFile.seek(start);
                targetFile.seek(start);
                int len = uploadFile.read(data);
                log.info("partNum = {}, partOffset = {}, partSize = {}", partNum, partOffset, partSize);
                targetFile.write(data, 0, len);

                downloadCheckPoint.update(partNum, true);
                downloadCheckPoint.dump();
            } catch (Exception e) {
                downloadPartResult.setFailed(true);
                downloadPartResult.setException(e);
            }

            return downloadPartResult;
        }
    }

    @Override
    public void delete(String targetName) {
        FileUtil.del(getKey(targetName, true));
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        FileUtil.copy(getKey(sourceName, true), getKey(targetName, true), isOverride);
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        FileUtil.move(Paths.get(getKey(sourceName, true)), Paths.get(getKey(targetName, true)), isOverride);
    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {
        FileUtil.rename(Paths.get(getKey(sourceName, true)), getKey(targetName, true), isOverride);
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {

        String key = getKey(targetName, true);
        File file = FileUtil.file(key);
        OssInfo ossInfo = getBaseInfo(file.getPath());
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(replaceKey(targetName, file.getName(), true));

        if (isRecursion && FileUtil.isDirectory(key)) {
            List<File> files = PathUtil.loopFiles(Paths.get(key), 1, pathname -> true);
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (File childFile : files) {
                String target = replaceKey(childFile.getPath(), getBasePath(), true);
                if (childFile.isFile()) {
                    fileOssInfos.add(getInfo(target, false));
                } else if (childFile.isDirectory()) {
                    directoryInfos.add(getInfo(target, true));
                }
            }
            ReflectUtil.setFieldValue(ossInfo, "fileInfos", fileOssInfos);
            ReflectUtil.setFieldValue(ossInfo, "directoryInfos", directoryInfos);
        }

        return ossInfo;
    }

    @Override
    public Boolean isExist(String targetName) {
        return FileUtil.exist(getKey(targetName, true));
    }

    @Override
    public Boolean isFile(String targetName) {
        return FileUtil.isFile(getKey(targetName, true));
    }

    @Override
    public Boolean isDirectory(String targetName) {
        return FileUtil.isDirectory(getKey(targetName, true));
    }

    public OssInfo getBaseInfo(String targetName) {
        OssInfo ossInfo = null;
        try {
            Path path = Paths.get(targetName);
            BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);

            FileTime lastModifiedTime = basicFileAttributes.lastModifiedTime();
            FileTime creationTime = basicFileAttributes.creationTime();
            long size = basicFileAttributes.size();
            UserPrincipal owner = Files.getOwner(path);

            if (FileUtil.isFile(targetName)) {
                ossInfo = new FileOssInfo();
            } else {
                ossInfo = new DirectoryOssInfo();
            }
            ossInfo.setLastUpdateTime(DateUtil.date(lastModifiedTime.toMillis()).toString(DatePattern.NORM_DATETIME_PATTERN));
            ossInfo.setCreateTime(DateUtil.date(creationTime.toMillis()).toString(DatePattern.NORM_DATETIME_PATTERN));
            ossInfo.setSize(Convert.toStr(size));
        } catch (Exception e) {
            log.error("获取{}文件属性失败", targetName, e);
        }

        return Optional.ofNullable(ossInfo).orElse(new FileOssInfo());
    }

    @Override
    public String getBasePath() {
        String basePath = localOssConfig.getBasePath();
        if (!FileUtil.exist(basePath)) {
            FileUtil.mkdir(basePath);
        }
        return basePath;
    }

}
