package io.github.artislong.core.up;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.upyun.ParallelUploader;
import com.upyun.RestManager;
import com.upyun.UpException;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.core.up.constant.UpConstant;
import io.github.artislong.core.up.model.UpOssConfig;
import io.github.artislong.exception.OssException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.utils.OssPathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.Response;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/upyun/java-sdk
 * @author 陈敏
 * @version UpOssClient.java, v 1.1 2021/11/30 12:03 chenmin Exp $
 * Created on 2021/11/30
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpOssClient implements StandardOssClient {

    public static final String REST_OBJECT_NAME = "restManager";
    public static final String PARALLEL_OBJECT_NAME = "parallelUploader";

    private RestManager restManager;
    private ParallelUploader parallelUploader;
    private UpOssConfig upOssConfig;

    @Override
    public OssInfo upload(InputStream inputStream, String targetName, boolean isOverride) {
        try {
            restManager.writeFile(getKey(targetName, true), inputStream, null);
        } catch (IOException | UpException e) {
            log.error("{}上传失败", targetName, e);
            throw new OssException(e);
        }
        return getInfo(targetName, false);
    }

    @Override
    public OssInfo uploadCheckPoint(File file, String targetName) {
        try {
            parallelUploader.upload(file.getPath(), getKey(targetName, true), null);
        } catch (Exception e) {
            throw new OssException(e);
        }
        return getInfo(targetName);
    }

    @Override
    public void download(OutputStream outputStream, String targetName) {
        try {
            Response response = restManager.readFile(getKey(targetName, true));
            IoUtil.copy(response.body().byteStream(), outputStream);
        } catch (IOException | UpException e) {
            log.error("{}下载失败", targetName, e);
            throw new OssException(e);
        }
    }

    @Override
    public void downloadcheckpoint(File localFile, String targetName) {
        log.warn("又拍云不支持断点续传下载，将使用普通下载");
        try (OutputStream os = new FileOutputStream(localFile)) {
            download(os, targetName);
        } catch (Exception e) {
            log.error("{}下载失败", targetName, e);
            throw new OssException(e);
        }
    }

    @Override
    public void delete(String targetName) {
        try {
            restManager.deleteFile(getKey(targetName, true), null);
        } catch (IOException | UpException e) {
            log.error("{}删除失败", targetName, e);
            throw new OssException(e);
        }
    }

    @Override
    public void copy(String sourceName, String targetName, boolean isOverride) {
        try {
            restManager.copyFile(getKey(targetName, true), getKey(sourceName, true), null);
        } catch (IOException | UpException e) {
            log.error("{}复制到{}失败", sourceName, targetName, e);
        }
    }

    @Override
    public void move(String sourceName, String targetName, boolean isOverride) {
        String newSourceName = getKey(sourceName, true);
        String newTargetName = getKey(targetName, true);
        try {
            if (isFile(newSourceName)) {
                restManager.moveFile(targetName, sourceName, null);
            } else {
                restManager.mkDir(newTargetName);
                restManager.rmDir(newSourceName);
            }
        } catch (IOException | UpException e) {
            log.error("{}移动到{}失败", sourceName, targetName, e);
            throw new OssException(e);
        }
    }

    @Override
    public OssInfo getInfo(String targetName, boolean isRecursion) {
        String key = getKey(targetName, true);
        try {
            OssInfo ossInfo = getBaseInfo(key);
            ossInfo.setName(StrUtil.equals(targetName, StrUtil.SLASH) ? targetName : FileNameUtil.getName(targetName));
            ossInfo.setPath(OssPathUtil.replaceKey(targetName, ossInfo.getName(), true));
            if (isRecursion && isDirectory(key)) {
                List<OssInfo> fileOssInfos = new ArrayList<>();
                List<OssInfo> directoryInfos = new ArrayList<>();
                Response response = restManager.readDirIter(key, null);
                IoUtil.readUtf8Lines(response.body().byteStream(), (LineHandler) line -> {
                    List<String> fields = StrUtil.split(line, "\t");  // vim.png N 164026 1638536314
                    if (UpConstant.FILE_TYPE.equals(fields.get(1))) {
                        fileOssInfos.add(getInfo(OssPathUtil.replaceKey(key + StrUtil.SLASH + fields.get(0), getBasePath(), true), true));
                    } else {
                        directoryInfos.add(getInfo(OssPathUtil.replaceKey(key + StrUtil.SLASH + fields.get(0), getBasePath(), true), true));
                    }
                });
                if (ObjectUtil.isNotEmpty(fileOssInfos) && fileOssInfos.get(0) instanceof FileOssInfo) {
                    ReflectUtil.setFieldValue(ossInfo, "fileInfos", fileOssInfos);
                }
                if (ObjectUtil.isNotEmpty(directoryInfos) && directoryInfos.get(0) instanceof DirectoryOssInfo) {
                    ReflectUtil.setFieldValue(ossInfo, "directoryInfos", directoryInfos);
                }
            }
            return ossInfo;
        } catch (IOException | UpException e) {
            log.error("获取{}基本信息失败", targetName, e);
            throw new OssException(e);
        }
    }

    @Override
    public boolean isExist(String targetName) {
        String key = getKey(targetName, true);
        try {
            if (isFile(targetName)) {
                Response response = restManager.getFileInfo(key);
                String fileSize = response.header(RestManager.PARAMS.X_UPYUN_FILE_SIZE.getValue(), "0");
                return Convert.toInt(fileSize) > 0;
            } else {
                DirectoryOssInfo ossInfo = getDirectoryOssInfo(key);
                return Convert.toInt(ossInfo.getLength()) > 0;
            }
        } catch (IOException | UpException e) {
            log.error("判断{}是否存在失败", targetName, e);
            return false;
        }
    }

    @Override
    public String getBasePath() {
        return upOssConfig.getBasePath();
    }

    @Override
    public Map<String, Object> getClientObject() {
        return new HashMap<String, Object>() {
            {
                put(REST_OBJECT_NAME, getRestManager());
                put(PARALLEL_OBJECT_NAME, getParallelUploader());
            }
        };
    }

    private OssInfo getBaseInfo(String key) throws UpException, IOException {
        OssInfo ossInfo;
        if (isFile(key)) {
            ossInfo = new FileOssInfo();
            Response fileInfo = restManager.getFileInfo(key);
            Headers headers = fileInfo.headers();
            ossInfo.setLength(headers.get(RestManager.PARAMS.X_UPYUN_FILE_SIZE.getValue()));
            ossInfo.setCreateTime(DateUtil.date(headers.getDate(RestManager.PARAMS.X_UPYUN_FILE_DATE.getValue())).toString(DatePattern.NORM_DATETIME_PATTERN));
            ossInfo.setLastUpdateTime(DateUtil.date(headers.getDate(RestManager.PARAMS.X_UPYUN_FILE_DATE.getValue())).toString(DatePattern.NORM_DATETIME_PATTERN));
        } else {
            ossInfo = getDirectoryOssInfo(key);
        }
        return ossInfo;
    }

    private DirectoryOssInfo getDirectoryOssInfo(String key) throws UpException, IOException {
        String name = FileNameUtil.getName(key);
        String newKey = OssPathUtil.replaceKey(key, name, true);

        Response response = restManager.readDirIter(newKey, null);

        DirectoryOssInfo ossInfo = new DirectoryOssInfo();
        IoUtil.readUtf8Lines(response.body().byteStream(), (LineHandler) line -> {
            List<String> fields = StrUtil.split(line, "\t");  // test Y 164026 1638536314
            if (name.equals(fields.get(0))) {
                ossInfo.setName(fields.get(0));
                ossInfo.setPath(OssPathUtil.replaceKey(newKey, getBasePath(), true));
                ossInfo.setLength(fields.get(2));
                ossInfo.setCreateTime(DateUtil.date(Convert.toLong(fields.get(3)) * 1000).toString(DatePattern.NORM_DATETIME_PATTERN));
                ossInfo.setLastUpdateTime(DateUtil.date(Convert.toLong(fields.get(3)) * 1000).toString(DatePattern.NORM_DATETIME_PATTERN));
            }
        });
        return ossInfo;
    }
}
