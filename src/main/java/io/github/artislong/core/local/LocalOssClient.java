package io.github.artislong.core.local;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import io.github.artislong.constant.OssType;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.local.model.LocalOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.model.SliceConfig;
import io.github.artislong.model.download.DownloadCheckPoint;
import io.github.artislong.model.download.DownloadObjectStat;
import io.github.artislong.model.upload.*;
import io.github.artislong.utils.OssPathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipal;
import java.util.*;

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
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        String key = getKey(targetName, true);
        if (isOverride && FileUtil.exist(key)) {
            FileUtil.del(key);
        }
        File file = FileUtil.writeFromStream(inputStream, key);

        OssInfo ossInfo = getBaseInfo(file.getPath());

        ossInfo.setName(file.getName());
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, file.getName(), true));
        return ossInfo;
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        uploadFile(file, targetName, localOssConfig.getSliceConfig(), OssType.LOCAL);
        return getInfo(targetName);
    }

    @Override
    public void prepareUpload(UploadCheckpoint uploadCheckPoint, File uploadfile, String targetName, String checkpointFile, SliceConfig slice) {
        String key = getKey(targetName, true);

        uploadCheckPoint.setMagic(UploadCheckpoint.UPLOAD_MAGIC);
        uploadCheckPoint.setUploadFile(uploadfile.getPath());
        uploadCheckPoint.setKey(key);
        uploadCheckPoint.setCheckpointFile(checkpointFile);
        uploadCheckPoint.setUploadFileStat(UploadFileStat.getFileStat(uploadCheckPoint.getUploadFile()));

        long partSize = localOssConfig.getSliceConfig().getPartSize();
        long fileLength = uploadfile.length();
        int parts = (int) (fileLength / partSize);
        if (fileLength % partSize > 0) {
            parts++;
        }

        uploadCheckPoint.setUploadParts(splitUploadFile(uploadCheckPoint.getUploadFileStat().getSize(), partSize));
        uploadCheckPoint.setOriginPartSize(parts);
    }

    @Override
    public UploadPartResult uploadPart(UploadCheckpoint uploadcheckpoint, int partNum, InputStream inputStream) {
        UploadPartResult partResult = null;
        UploadPart uploadPart = uploadcheckpoint.getUploadParts().get(partNum);
        long offset = uploadPart.getOffset();
        long size = uploadPart.getSize();
        Integer partSize = Convert.toInt(size);

        partResult = new UploadPartResult(partNum + 1, offset, size);
        partResult.setNumber(partNum);
        try {

            RandomAccessFile targetFile = new RandomAccessFile(uploadcheckpoint.getKey(), "rw");

            byte[] data = new byte[partSize];
            inputStream.skip(offset);
            targetFile.seek(offset);
            int len = targetFile.read(data);
            targetFile.write(data, 0, len);
            partResult.setEntityTag(new UploadPartEntityTag());
        } catch (Exception e) {
            partResult.setFailed(true);
            partResult.setException(e);
        }

        return partResult;
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {
        FileUtil.writeToStream(getKey(targetName, true), outputStream);
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        downloadfile(localFile, targetName, localOssConfig.getSliceConfig(), OssType.LOCAL);
    }

    @Override
    public DownloadObjectStat getDownloadObjectStat(String targetName) {
        File file = new File(getKey(targetName, true));
        return new DownloadObjectStat()
                .setSize(file.length())
                .setLastModified(new Date(file.lastModified()))
                .setDigest(DigestUtil.sha256Hex(Convert.toStr(file.lastModified())));
    }

    @Override
    public void prepareDownload(DownloadCheckPoint downloadCheckPoint, File localFile, String targetName, String checkpointFile) {
        downloadCheckPoint.setMagic(DownloadCheckPoint.DOWNLOAD_MAGIC);
        downloadCheckPoint.setDownloadFile(localFile.getPath());
        downloadCheckPoint.setKey(getKey(targetName, false));
        downloadCheckPoint.setCheckPointFile(checkpointFile);

        downloadCheckPoint.setObjectStat(getDownloadObjectStat(targetName));

        long downloadSize;
        if (downloadCheckPoint.getObjectStat().getSize() > 0) {
            Long partSize = localOssConfig.getSliceConfig().getPartSize();
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
    public InputStream downloadPart(String key, long start, long end) {
        try {
            RandomAccessFile uploadFile = new RandomAccessFile(key, "r");
            byte[] data = new byte[Convert.toInt(end - start)];
            uploadFile.seek(start);
            uploadFile.read(data);
            return new ByteArrayInputStream(data);
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public void delete(String targetName) {
        FileUtil.del(getKey(targetName, true));
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        FileUtil.copy(getKey(sourceName, true), getKey(targetName, true), isOverride);
    }

    @Override
    public void move(String sourceName, String targetName, boolean isOverride) {
        FileUtil.move(Paths.get(getKey(sourceName, true)), Paths.get(getKey(targetName, true)), isOverride);
    }

    @Override
    public void rename(String sourceName, String targetName, boolean isOverride) {
        FileUtil.rename(Paths.get(getKey(sourceName, true)), getKey(targetName, true), isOverride);
    }

    @Override
    public OssInfo getInfo(String targetName, boolean isRecursion) {

        String key = getKey(targetName, true);
        File file = FileUtil.file(key);
        OssInfo ossInfo = getBaseInfo(file.getPath());
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, file.getName(), true));

        if (isRecursion && FileUtil.isDirectory(key)) {
            List<File> files = PathUtil.loopFiles(Paths.get(key), 1, pathname -> true);
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (File childFile : files) {
                String target = OssPathUtil.replaceKey(childFile.getPath(), getBasePath(), true);
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
    public boolean isExist(String targetName) {
        return FileUtil.exist(getKey(targetName, true));
    }

    @Override
    public boolean isFile(String targetName) {
        return FileUtil.isFile(getKey(targetName, true));
    }

    @Override
    public boolean isDirectory(String targetName) {
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
            ossInfo.setLength(Convert.toStr(size));
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

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<>();
    }
}
