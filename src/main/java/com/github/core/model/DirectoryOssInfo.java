package com.github.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件夹信息对象
 * @author 陈敏
 * @version DirectoryInfo.java, v 1.1 2021/11/15 10:21 chenmin Exp $
 * Created on 2021/11/15
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DirectoryOssInfo extends OssInfo {

    /**
     * 文件夹列表
     */
    private List<FileOssInfo> fileInfos = new ArrayList<>();

    /**
     * 文件列表
     */
    private List<DirectoryOssInfo> directoryInfos = new ArrayList<>();

}
