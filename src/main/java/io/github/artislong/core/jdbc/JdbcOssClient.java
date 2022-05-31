package io.github.artislong.core.jdbc;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.jdbc.adapter.JdbcOssOperation;
import io.github.artislong.core.jdbc.constant.JdbcOssConstant;
import io.github.artislong.core.jdbc.model.JdbcOssConfig;
import io.github.artislong.core.jdbc.model.JdbcOssInfo;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.utils.OssPathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 陈敏
 * @version JdbcOssClient.java, v 1.0 2022/3/11 21:34 chenmin Exp $
 * Created on 2022/3/11
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JdbcOssClient implements StandardOssClient {

    public static final String JDBC_OBJECT_NAME = "jdbcOssOperation";

    private JdbcOssConfig jdbcOssConfig;
    private JdbcOssOperation jdbcOssOperation;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        try {
            String dateId = jdbcOssOperation.saveOssData(inputStream);
            String key = getKey(targetName, true);
            Long size = Convert.toLong(inputStream.available());
            JdbcOssInfo jdbcOssInfo;
            if (isExist(targetName) && isOverride) {
                jdbcOssInfo = jdbcOssOperation.getOssInfo(key);
                jdbcOssOperation.updateOssData(jdbcOssInfo.getDataId(), inputStream);
                jdbcOssOperation.updateOssInfo(jdbcOssInfo.getId(), key, size, jdbcOssInfo.getParentId());
                jdbcOssInfo.setSize(size);
                jdbcOssInfo.setLastUpdateTime(new Date());
            } else {
                String parentPath = OssPathUtil.convertPath(Paths.get(key).getParent().toString(), true);
                String parentId = mkdir(parentPath);
                jdbcOssInfo = jdbcOssOperation.saveOssInfo(key, size, parentId, JdbcOssConstant.OSS_TYPE.FILE, dateId);
            }
            OssInfo ossInfo = jdbcOssInfo.convertOssInfo(getBasePath());
            ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
            ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));
            return ossInfo;
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        log.warn("Jdbc存储不支持断点续传上传，将使用普通上传");
        return upload(FileUtil.getInputStream(file), targetName);
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {
        JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(getKey(targetName, true));
        try {
            InputStream inputStream = jdbcOssOperation.getOssData(jdbcOssInfo.getDataId());
            IoUtil.copy(inputStream, outputStream);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        log.warn("Jdbc存储不支持断点续传下载，将使用普通下载");
        download(FileUtil.getOutputStream(localFile), targetName);
    }

    @Override
    public void delete(String targetName) {
        String key = getKey(targetName, true);
        JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(key);
        jdbcOssOperation.deleteOssData(jdbcOssInfo.getDataId());
        jdbcOssOperation.deleteOssInfo(jdbcOssInfo.getId());
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        if (isOverride || !isExist(targetName)) {
            String sourceKey = getKey(sourceName, true);
            String targetKey = getKey(targetName, true);
            JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(sourceKey);
            String targetDataId = jdbcOssOperation.copyOssData(jdbcOssInfo.getDataId());
            jdbcOssOperation.copyOssInfo(jdbcOssInfo.getId(), targetKey, targetDataId);
        }
    }

    @Override
    public void move(String sourceName, String targetName, boolean isOverride) {
        if (isOverride || !isExist(targetName)) {
            String sourceKey = getKey(sourceName, true);
            String targetKey = getKey(targetName, true);
            JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(sourceKey);
            String targetParentId = mkdir(targetKey);
            jdbcOssOperation.updateOssInfo(jdbcOssInfo.getId(), targetKey, jdbcOssInfo.getSize(), targetParentId);
        }
    }

    @Override
    public void rename(String sourceName, String targetName, boolean isOverride) {
        if (isOverride || !isExist(targetName)) {
            String sourceKey = getKey(sourceName, true);
            String targetKey = getKey(targetName, true);
            JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(sourceKey);
            String parentPath = OssPathUtil.convertPath(Paths.get(targetKey).getParent().toString(), true);
            String targetParentId = mkdir(parentPath);
            jdbcOssOperation.updateOssInfo(jdbcOssInfo.getId(), targetKey, jdbcOssInfo.getSize(), targetParentId);
        }
    }

    @Override
    public boolean isExist(String targetName) {
        JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(getKey(targetName, true));
        return ObjectUtil.isNotEmpty(jdbcOssInfo) && jdbcOssInfo.getSize() > 0;
    }

    @Override
    public boolean isFile(String targetName) {
        JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(getKey(targetName, true));
        return JdbcOssConstant.OSS_TYPE.FILE.equals(jdbcOssInfo.getType());
    }

    @Override
    public boolean isDirectory(String targetName) {
        JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(getKey(targetName, true));
        return JdbcOssConstant.OSS_TYPE.DIRECTORY.equals(jdbcOssInfo.getType());
    }

    @Override
    public OssInfo getInfo(String targetName, boolean isRecursion) {
        String key = getKey(targetName, true);
        JdbcOssInfo baseJdbcOssInfo = jdbcOssOperation.getOssInfo(key);

        OssInfo ossInfo = baseJdbcOssInfo.convertOssInfo(getBasePath());
        ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
        ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));

        if (JdbcOssConstant.OSS_TYPE.DIRECTORY.equals(baseJdbcOssInfo.getType()) && isRecursion) {
            String basePath = "";
            if ("0".equals(baseJdbcOssInfo.getParentId())) {
                basePath = baseJdbcOssInfo.getPath();
            } else {
                basePath = baseJdbcOssInfo.getPath() + baseJdbcOssInfo.getName();
            }
            List<JdbcOssInfo> jdbcOssInfos = jdbcOssOperation.getOssInfos(basePath);
            Map<String, List<JdbcOssInfo>> jdbcOssInfoMaps = jdbcOssInfos.stream().collect(Collectors.groupingBy(JdbcOssInfo::getParentId));
            ossInfo = getChildren(jdbcOssInfoMaps, baseJdbcOssInfo.getId(), ossInfo);
        }
        return ossInfo;
    }

    public OssInfo getChildren(Map<String, List<JdbcOssInfo>> jdbcOssInfoMaps, String parentId, OssInfo ossInfo) {
        List<OssInfo> fileOssInfos = new ArrayList<>();
        List<OssInfo> directoryInfos = new ArrayList<>();

        List<JdbcOssInfo> children = jdbcOssInfoMaps.get(parentId);
        for (JdbcOssInfo child : children) {
            if (JdbcOssConstant.OSS_TYPE.DIRECTORY.equals(child.getType())) {
                OssInfo directoryOssInfo = child.convertOssInfo(getBasePath());
                directoryInfos.add(getChildren(jdbcOssInfoMaps, child.getId(), directoryOssInfo));
            } else {
                OssInfo fileOssInfo = child.convertOssInfo(getBasePath());
                fileOssInfos.add(fileOssInfo);
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
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(JDBC_OBJECT_NAME, getJdbcOssOperation());
            }
        };
    }

    @Override
    public String getBasePath() {
        return jdbcOssConfig.getBasePath();
    }

    public String mkdir(String path) {
        List<String> paths = StrUtil.split(path, StrUtil.SLASH, false, false);
        StringBuilder fullPath = new StringBuilder();
        JdbcOssInfo parentOssInfo = null;
        for (int i = 0; i < paths.size(); i++) {
            String pathName = StrUtil.SLASH + paths.get(i);
            if (i != 0) {
                fullPath.append(pathName);
            }

            String key = ObjectUtil.isEmpty(fullPath.toString()) ? StrUtil.SLASH : fullPath.toString();

            JdbcOssInfo jdbcOssInfo = jdbcOssOperation.getOssInfo(key);
            if (jdbcOssInfo == null) {
                String parentId = parentOssInfo == null ? "0" : parentOssInfo.getId();
                jdbcOssInfo = jdbcOssOperation.saveOssInfo(key, 0L, parentId, JdbcOssConstant.OSS_TYPE.DIRECTORY, "");
            }
            parentOssInfo = jdbcOssInfo;
        }
        return parentOssInfo.getId();
    }

}
