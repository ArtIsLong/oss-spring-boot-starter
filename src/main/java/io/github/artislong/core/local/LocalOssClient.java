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
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.model.DirectoryOssInfo;
import io.github.artislong.core.model.FileOssInfo;
import io.github.artislong.core.model.OssInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    public OssInfo createFile(String targetName) {
        File file = FileUtil.touch(getKey(targetName, true));
        OssInfo ossInfo = getBaseInfo(file.getPath());
        ossInfo.setName(file.getName());
        ossInfo.setPath(replaceKey(targetName, file.getName(), true));
        return ossInfo;
    }

    @Override
    public OssInfo createDirectory(String targetName) {
        File file = FileUtil.mkdir(getKey(targetName, true));
        OssInfo ossInfo = getBaseInfo(file.getPath());
        ossInfo.setName(file.getName());
        ossInfo.setPath(replaceKey(targetName, file.getName(), true));
        return ossInfo;
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
            ossInfo.setCreater(owner.getName());

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
