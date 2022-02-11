package io.github.artislong.core;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.OssProperties;
import io.github.artislong.model.OssInfo;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author 陈敏
 * @version AbstractOssClient.java, v 1.1 2021/11/5 15:44 chenmin Exp $
 * Created on 2021/11/5
 */
public interface StandardOssClient {

    /**
     * 上传文件，默认覆盖
     * @param is 输入流
     * @param targetName 目标文件路径
     * @return 文件信息
     */
    default OssInfo upLoad(InputStream is, String targetName) {
        return upLoad(is, targetName, true);
    }

    /**
     * 上传文件
     * @param is 输入流
     * @param targetName 目标文件路径
     * @param isOverride 是否覆盖
     * @return 文件信息
     */
    OssInfo upLoad(InputStream is, String targetName, Boolean isOverride);

    /**
     * 断点续传
     * @param file 本地文件路径
     * @param targetName  目标文件路径
     * @return 文件信息
     */
    default OssInfo upLoadCheckPoint(String file, String targetName) {
        return upLoadCheckPoint(new File(file), targetName);
    }

    /**
     * 断点续传
     * @param file 本地文件
     * @param targetName 目标文件路径
     * @return 文件信息
     */
    OssInfo upLoadCheckPoint(File file, String targetName);

    /**
     * 下载文件
     * @param os  输出流
     * @param targetName  目标文件路径
     */
    void downLoad(OutputStream os, String targetName);

    /**
     * 断点续传
     * @param localFile 本地文件路径
     * @param targetName 目标文件路径
     * @return 文件信息
     */
    default void downLoadCheckPoint(String localFile, String targetName) {
        downLoadCheckPoint(new File(localFile), targetName);
    }

    /**
     * 断点续传
     * @param localFile 本地文件
     * @param targetName 目标文件路径
     * @return 文件信息
     */
    default void downLoadCheckPoint(File localFile, String targetName) {}

    /**
     * 删除文件
     * @param targetName 目标文件路径
     */
    void delete(String targetName);

    /**
     * 复制文件，默认覆盖
     * @param sourceName 源文件路径
     * @param targetName 目标文件路径
     */
    default void copy(String sourceName, String targetName) {
        copy(sourceName, targetName, true);
    }

    /**
     * 复制文件
     * @param sourceName 源文件路径
     * @param targetName 目标文件路径
     * @param isOverride 是否覆盖
     */
    void copy(String sourceName, String targetName, Boolean isOverride);

    /**
     * 移动文件，默认覆盖
     * @param sourceName 源文件路径
     * @param targetName 目标路径
     */
    default void move(String sourceName, String targetName) {
        move(sourceName, targetName, true);
    }

    /**
     * 移动文件
     * @param sourceName 源文件路径
     * @param targetName 目标路径
     * @param isOverride 是否覆盖
     */
    default void move(String sourceName, String targetName, Boolean isOverride) {
        copy(sourceName, targetName, isOverride);
        delete(sourceName);
    }

    /**
     * 重命名文件
     * @param sourceName 源文件路径
     * @param targetName 目标文件路径
     */
    default void rename(String sourceName, String targetName) {
        rename(sourceName, targetName, true);
    }

    /**
     * 重命名文件
     * @param sourceName 源文件路径
     * @param targetName 目标路径
     * @param isOverride 是否覆盖
     */
    default void rename(String sourceName, String targetName, Boolean isOverride) {
        move(sourceName, targetName, isOverride);
    }

    /**
     * 获取文件信息，默认获取目标文件信息
     * @param targetName 目标文件路径
     * @return 文件基本信息
     */
    default OssInfo getInfo(String targetName) {
        return getInfo(targetName, false);
    }

    /**
     * 获取文件信息
     *      isRecursion传false，则只获取当前对象信息；
     *      isRecursion传true，且当前对象为目录时，会递归获取当前路径下所有文件及目录，按层级返回
     * @param targetName 目标文件路径
     * @param isRecursion 是否递归
     * @return 文件基本信息
     */
    OssInfo getInfo(String targetName, Boolean isRecursion);

    /**
     * 是否存在
     * @param targetName 目标文件路径
     * @return true/false
     */
    default Boolean isExist(String targetName) {
        OssInfo info = getInfo(targetName);
        return ObjectUtil.isNotEmpty(info) && ObjectUtil.isNotEmpty(info.getName());
    }

    /**
     * 是否为文件
     *      默认根据路径最后一段名称是否有后缀名来判断是否为文件，此方式不准确，当存储平台不提供类似方法时，可使用此方法
     * @param targetName 目标文件路径
     * @return true/false
     */
    default Boolean isFile(String targetName) {
        String name = FileNameUtil.getName(targetName);
        return StrUtil.indexOf(name, StrUtil.C_DOT) > 0;
    }

    /**
     * 是否为目录
     *      与判断是否为文件相反
     * @param targetName 目标文件路径
     * @return true/false
     */
    default Boolean isDirectory(String targetName) {
        return !isFile(targetName);
    }

    /**
     * 路径转换
     *  将路径分隔符转为统一的 / 分隔
     * @param key 路径
     * @param isAbsolute 是否绝对路径
     *                    true：绝对路径；false：相对路径
     * @return 以 / 为分隔的路径
     */
    default String convertPath(String key, Boolean isAbsolute) {
        key = key.replaceAll("\\\\", StrUtil.SLASH).replaceAll("//", StrUtil.SLASH);
        if (isAbsolute && !key.startsWith(StrUtil.SLASH)) {
            key = StrUtil.SLASH + key;
        } else if (!isAbsolute && key.startsWith(StrUtil.SLASH)) {
            key = key.replaceFirst(StrUtil.SLASH, "");
        }
        return key;
    }

    /**
     * 获取完整Key
     * @param targetName 目标地址
     * @param isAbsolute 是否绝对路径
     *                   true：绝对路径；false：相对路径
     * @return 完整路径
     */
    default String getKey(String targetName, Boolean isAbsolute) {
        return convertPath(getBasePath() + targetName, isAbsolute);
    }

    /**
     * 获取相对根路径的绝对路径
     * @param path 全路径
     * @param basePath  根路径
     * @param isAbsolute  是否绝对路径
     *                   true：绝对路径；false：相对路径
     * @return 完整路径
     */
    default String replaceKey(String path, String basePath, Boolean isAbsolute) {
        String newPath;
        if (StrUtil.SLASH.equals(basePath)) {
            newPath = convertPath(path, isAbsolute);
        } else {
            newPath = convertPath(path, isAbsolute).replaceAll(convertPath(basePath, isAbsolute), "");
        }
        return convertPath(newPath, isAbsolute);
    }

    /**
     * 获取文件存储根路径
     * @return 根路径
     */
    default String getBasePath() {
        return getOssProperties().getBasePath();
    }

    /**
     * 注入OssProperties对象
     * @param ossProperties OssProperties对象
     */
    void setOssProperties(OssProperties ossProperties);

    /**
     * 获取OssProperties对象
     * @return OssProperties对象
     */
    OssProperties getOssProperties();

}
