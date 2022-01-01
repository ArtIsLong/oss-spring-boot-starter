package io.github.artislong.core.ftp;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.ftp.Ftp;
import io.github.artislong.OssProperties;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version FtpOssClient.java, v 1.1 2021/11/15 11:11 chenmin Exp $
 * Created on 2021/11/15
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FtpOssClient implements StandardOssClient {

    private Ftp ftp;
    private OssProperties ossProperties;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String key = getKey(targetName, true);
        String parentPath = convertPath(Paths.get(key).getParent().toString(), true);
        if (!ftp.exist(parentPath)) {
            ftp.mkDirs(parentPath);
        }
        if (isOverride || !ftp.exist(key)) {
            ftp.upload(parentPath, FileNameUtil.getName(targetName), is);
        }
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        String key = getKey(targetName, true);
        ftp.download(convertPath(Paths.get(key).getParent().toString(), true), key, os);
    }

    @Override
    public void delete(String targetName) {
        String key = getKey(targetName, true);
        if (isDirectory(targetName)) {
            ftp.delDir(key);
        } else {
            ftp.delFile(key);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        // TODO ftp协议不支持copy命令，暂不实现
        log.warn("ftp协议不支持copy命令，暂不实现");
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        // TODO ftp协议不支持move命令，暂不实现
        log.warn("ftp协议不支持move命令，暂不实现");
    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {
        String newSourceName = getKey(sourceName, true);
        String newTargetName = getKey(targetName, true);
        try {
            if (isOverride || !isExist(newTargetName)) {
                ftp.getClient().rename(newSourceName, newTargetName);
            }
        } catch (IOException e) {
            log.error("{}重命名为{}失败,错误信息为：", newSourceName, newTargetName, e);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, true);
        OssInfo ossInfo = getBaseInfo(key);
        if (isRecursion && ftp.isDir(key)) {
            FTPFile[] ftpFiles = ftp.lsFiles(key);
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (FTPFile ftpFile : ftpFiles) {
                if (ftpFile.isDirectory()) {
                    directoryInfos.add(getInfo(targetName + CharPool.SLASH + ftpFile.getName(), true));
                } else {
                    fileOssInfos.add(getInfo(targetName + CharPool.SLASH + ftpFile.getName(), false));
                }
            }
            ReflectUtil.setFieldValue(ossInfo, "fileInfos", fileOssInfos);
            ReflectUtil.setFieldValue(ossInfo, "directoryInfos", directoryInfos);
        }
        return ossInfo;
    }

    @Override
    public Boolean isExist(String targetName) {
        return ftp.exist(getKey(targetName, true));
    }

    @Override
    public Boolean isFile(String targetName) {
        return !isDirectory(targetName);
    }

    @Override
    public Boolean isDirectory(String targetName) {
        return ftp.isDir(getKey(targetName, true));
    }

    @Override
    public OssInfo createDirectory(String targetName) {
        ftp.mkDirs(getKey(targetName, true));
        return getInfo(targetName);
    }

    private OssInfo getBaseInfo(String targetName) {
        String name = FileNameUtil.getName(targetName);
        String path = replaceKey(targetName, name, true);
        FTPFile targetFtpFile = null;
        OssInfo ossInfo;
        if (ftp.isDir(targetName)) {
            ossInfo = new DirectoryOssInfo();
            FTPFile[] ftpFiles = ftp.lsFiles(convertPath(Paths.get(targetName).getParent().toString(), true));
            for (FTPFile ftpFile : ftpFiles) {
                if (ftpFile.getName().equals(name)) {
                    targetFtpFile = ftpFile;
                    break;
                }
            }
        } else {
            ossInfo = new FileOssInfo();
            FTPFile[] ftpFiles = ftp.lsFiles(targetName);
            if (ArrayUtil.isNotEmpty(ftpFiles)) {
                targetFtpFile = ftpFiles[0];
            }
        }
        if (ObjectUtil.isNotEmpty(targetFtpFile)) {
            if (targetFtpFile.isFile()) {
                ossInfo = new FileOssInfo();
            }
            ossInfo.setName(name);
            ossInfo.setPath(path);
            ossInfo.setSize(Convert.toStr(targetFtpFile.getSize()));
            ossInfo.setCreateTime(DateUtil.date(targetFtpFile.getTimestamp()).toString(DatePattern.NORM_DATETIME_PATTERN));
            ossInfo.setLastUpdateTime(DateUtil.date(targetFtpFile.getTimestamp()).toString(DatePattern.NORM_DATETIME_PATTERN));
        }
        return ossInfo;
    }
}
