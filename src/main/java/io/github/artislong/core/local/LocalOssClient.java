package io.github.artislong.core.local;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.OssProperties;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.slice.*;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 本地文件操作客户端
 * @author 陈敏
 * @version LocalOssClient.java, v 1.1 2021/11/5 15:44 chenmin Exp $
 * Created on 2021/11/5
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalOssClient implements StandardOssClient {

    private OssProperties ossProperties;
    private LocalProperties localProperties;

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
            prepare(upLoadCheckPoint, upLoadFile, targetName, checkpointFile);
            FileUtil.del(checkpointFile);
        }

        Integer taskNum = getLocalProperties().getSliceConfig().getTaskNum();

        ExecutorService executorService = Executors.newFixedThreadPool(taskNum);
        List<Future<PartResult>> futures = new ArrayList<>();

        for (int i = 0; i < upLoadCheckPoint.getUploadParts().size(); i++) {
            if (!upLoadCheckPoint.getUploadParts().get(i).isCompleted()) {
                futures.add(executorService.submit(new UploadPartTask(upLoadCheckPoint, i)));
            }
        }

        executorService.shutdown();

        for (int i = 0; i < futures.size(); i++) {
            Future<PartResult> future = futures.get(i);
            try {
                PartResult partResult = future.get();
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

    private void prepare(UpLoadCheckPoint uploadCheckPoint, File upLoadFile, String targetName, String checkpointFile) {
        String key = getKey(targetName, true);

        uploadCheckPoint.setMagic(UpLoadCheckPoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(upLoadFile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setCheckpointFile(checkpointFile);
        uploadCheckPoint.setUploadFileStat(FileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = getLocalProperties().getSliceConfig().getPartSize();
        long fileLength = upLoadFile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitFile(uploadCheckPoint.getUploadFileStat().getSize(), parts));
        uploadCheckPoint.setOriginPartSize(parts);
    }

    private ArrayList<UploadPart> splitFile(long fileSize, long partSize) {
        ArrayList<UploadPart> parts = new ArrayList<>();

        long partNum = fileSize / partSize;
        if (partNum >= 10000) {
            partSize = fileSize / (10000 - 1);
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
    public static class UploadPartTask implements Callable<PartResult> {
        private UpLoadCheckPoint upLoadCheckPoint;
        private int partNum;

        UploadPartTask(UpLoadCheckPoint upLoadCheckPoint, int partNum) {
            this.upLoadCheckPoint = upLoadCheckPoint;
            this.partNum = partNum;
        }

        @Override
        public PartResult call() {
            PartResult partResult = null;
            try {
                UploadPart uploadPart = upLoadCheckPoint.getUploadParts().get(partNum);
                long offset = uploadPart.getOffset();
                long size = uploadPart.getSize();
                Integer partOffset = Convert.toInt(offset);
                Integer partSize = Convert.toInt(size);

                partResult = new PartResult(partNum + 1, offset, size);
                partResult.setNumber(partNum);

                RandomAccessFile uploadFile = new RandomAccessFile(upLoadCheckPoint.getUploadFile(), "r");
                RandomAccessFile targetFile = new RandomAccessFile(upLoadCheckPoint.getKey(), "rw");

                byte[] data = new byte[partSize];
                uploadFile.seek(offset);
                targetFile.seek(offset);
                int len = uploadFile.read(data);
                log.info("partNum = {}, partOffset = {}, partSize = {}", partNum, partOffset, partSize);
                targetFile.write(data, 0, len);

                upLoadCheckPoint.update(partNum, new PartEntityTag(), true);
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
        String basePath = ossProperties.getBasePath();
        if (!FileUtil.exist(basePath)) {
            FileUtil.mkdir(basePath);
        }
        return basePath;
    }

}
