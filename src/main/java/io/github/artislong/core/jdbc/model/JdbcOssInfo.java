package io.github.artislong.core.jdbc.model;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.core.jdbc.constant.JdbcOssConstant;
import io.github.artislong.exception.NotSupportException;
import io.github.artislong.model.DirectoryOssInfo;
import io.github.artislong.model.FileOssInfo;
import io.github.artislong.model.OssInfo;
import io.github.artislong.utils.OssPathUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author 陈敏
 * @version JdbcOssInfo.java, v 1.0 2022/3/12 15:49 chenmin Exp $
 * Created on 2022/3/12
 */
@Data
@Accessors(chain = true)
public class JdbcOssInfo {
    private String id;
    private String name;
    private String path;
    private Long size = 0L;
    private Date createTime;
    private Date lastUpdateTime;
    private String parentId;
    private String type;
    private String dataId;

    public OssInfo convertOssInfo(String basePath) {
        if (basePath.endsWith(StrUtil.SLASH)) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        String key = this.getPath() + this.getName();
        String path = OssPathUtil.replaceKey(this.getPath(), basePath, true);

        OssInfo ossInfo;
        switch (this.getType()) {
            case JdbcOssConstant.OSS_TYPE.FILE:
                ossInfo = new FileOssInfo();
                ossInfo.setPath(path);
                ossInfo.setLength(Convert.toStr(this.getSize()));
                break;
            case JdbcOssConstant.OSS_TYPE.DIRECTORY:
                ossInfo = new DirectoryOssInfo();
                if (key.equals(basePath)) {
                    ossInfo.setPath(StrUtil.SLASH);
                } else {
                    ossInfo.setPath(path);
                }
                break;
            default:
                throw new NotSupportException("不支持的对象类型");
        }

        ossInfo.setName(this.getName());
        ossInfo.setCreateTime(DateUtil.date(this.getCreateTime()).toString(DatePattern.NORM_DATETIME_PATTERN));
        ossInfo.setLastUpdateTime(DateUtil.date(this.getLastUpdateTime()).toString(DatePattern.NORM_DATETIME_PATTERN));
        return ossInfo;
    }
}
