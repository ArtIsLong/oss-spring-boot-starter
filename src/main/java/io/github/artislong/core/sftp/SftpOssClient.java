package io.github.artislong.core.sftp;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.sftp.model.SftpOssConfig;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version SftpOssClient.java, v 1.1 2021/11/15 11:12 chenmin Exp $
 * Created on 2021/11/15
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SftpOssClient implements StandardOssClient {

    private Sftp sftp;
    private SftpOssConfig sftpOssConfig;

    @Override
    public OssInfo upLoad(InputStream is, String targetName, Boolean isOverride) {
        String key = getKey(targetName, true);
        String parentPath = convertPath(Paths.get(key).getParent().toString(), true);
        if (!sftp.exist(parentPath)) {
            sftp.mkDirs(parentPath);
        }
        if (isOverride || !sftp.exist(key)) {
            sftp.upload(parentPath, FileNameUtil.getName(targetName), is);
        }
        return getInfo(targetName);
    }

    @Override
    public OssInfo upLoadCheckPoint(File file, String targetName) {
        String key = getKey(targetName, true);
        String parentPath = convertPath(Paths.get(key).getParent().toString(), true);
        if (!sftp.exist(parentPath)) {
            sftp.mkDirs(parentPath);
        }
        sftp.put(file.getPath(), parentPath, Sftp.Mode.RESUME);
        return getInfo(targetName);
    }

    @Override
    public void downLoad(OutputStream os, String targetName) {
        sftp.download(getKey(targetName, true), os);
    }

    @Override
    public void delete(String targetName) {
        String key = getKey(targetName, true);
        if (isDirectory(targetName)) {
            sftp.delDir(key);
        } else {
            sftp.delFile(key);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, Boolean isOverride) {
        // TODO sftp协议不支持copy命令
        log.warn("sftp协议不支持copy命令");
    }

    @Override
    public void move(String sourceName, String targetName, Boolean isOverride) {
        // TODO sftp协议不支持move命令
        log.warn("sftp协议不支持move命令");
    }

    @Override
    public void rename(String sourceName, String targetName, Boolean isOverride) {
        String newSourceName = getKey(sourceName, true);
        String newTargetName = getKey(targetName, true);
        try {
            if (isOverride || !isExist(newTargetName)) {
                sftp.getClient().rename(newSourceName, newTargetName);
            }
        } catch (SftpException e) {
            log.error("{}重命名为{}失败,错误信息为：", newSourceName, newTargetName, e);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, Boolean isRecursion) {
        String key = getKey(targetName, true);
        OssInfo ossInfo = getBaseInfo(key);
        if (isRecursion && sftp.isDir(key)) {
            List<ChannelSftp.LsEntry> lsEntries = sftp.lsEntries(key);
            List<OssInfo> fileOssInfos = new ArrayList<>();
            List<OssInfo> directoryInfos = new ArrayList<>();
            for (ChannelSftp.LsEntry lsEntry : lsEntries) {
                SftpATTRS attrs = lsEntry.getAttrs();
                String target = convertPath(targetName + CharPool.SLASH + lsEntry.getFilename(), true);
                if (attrs.isDir()) {
                    directoryInfos.add(getInfo(target, true));
                } else {
                    fileOssInfos.add(getInfo(target, false));
                }
            }
            ReflectUtil.setFieldValue(ossInfo, "fileInfos", fileOssInfos);
            ReflectUtil.setFieldValue(ossInfo, "directoryInfos", directoryInfos);
        }
        return ossInfo;
    }

    @Override
    public Boolean isExist(String targetName) {
        return sftp.exist(getKey(targetName, true));
    }

    @Override
    public Boolean isFile(String targetName) {
        return !isDirectory(targetName);
    }

    @Override
    public Boolean isDirectory(String targetName) {
        return sftp.isDir(getKey(targetName, true));
    }

    @Override
    public String getBasePath() {
        return sftpOssConfig.getBasePath();
    }

    private OssInfo getBaseInfo(String targetName) {
        String name = FileNameUtil.getName(targetName);
        String path = replaceKey(name, getBasePath(), true);
        ChannelSftp.LsEntry targetLsEntry = null;
        OssInfo ossInfo;
        if (sftp.isDir(targetName)) {
            ossInfo = new DirectoryOssInfo();
            List<ChannelSftp.LsEntry> lsEntries = sftp.lsEntries(convertPath(Paths.get(targetName).getParent().toString(), true));
            for (ChannelSftp.LsEntry lsEntry : lsEntries) {
                if (lsEntry.getFilename().equals(name)) {
                    targetLsEntry = lsEntry;
                    break;
                }
            }
        } else {
            ossInfo = new FileOssInfo();
            List<ChannelSftp.LsEntry> lsEntries = sftp.lsEntries(targetName);
            if (!lsEntries.isEmpty()) {
                targetLsEntry = lsEntries.get(0);
            }
        }
        if (ObjectUtil.isNotEmpty(targetLsEntry)) {
            SftpATTRS sftpattrs = targetLsEntry.getAttrs();
            if (!sftpattrs.isDir()) {
                ossInfo = new FileOssInfo();
            }
            ossInfo.setName(name);
            ossInfo.setPath(path);
            ossInfo.setSize(Convert.toStr(sftpattrs.getSize()));
            ossInfo.setCreateTime(DateUtil.date(sftpattrs.getMTime() * 1000L).toString(DatePattern.NORM_DATETIME_PATTERN));
            ossInfo.setLastUpdateTime(DateUtil.date(sftpattrs.getATime() * 1000L).toString(DatePattern.NORM_DATETIME_PATTERN));
        }
        return ossInfo;
    }
}
