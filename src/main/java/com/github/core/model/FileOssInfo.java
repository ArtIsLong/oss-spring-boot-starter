package com.github.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 文件信息对象
 * @author 陈敏
 * @version FileInfo.java, v 1.1 2021/11/15 10:19 chenmin Exp $
 * Created on 2021/11/15
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FileOssInfo extends OssInfo {

}
