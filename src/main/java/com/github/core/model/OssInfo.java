package com.github.core.model;

import lombok.Data;

/**
 * @author 陈敏
 * @version Info.java, v 1.1 2021/11/15 10:16 chenmin Exp $
 * Created on 2021/11/15
 */
@Data
public class OssInfo {

    /**
     * 名称
     */
    private String name;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 对象大小
     */
    private String size;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 最新修改时间
     */
    private String lastUpdateTime;
    /**
     * 创建人
     */
    private String creater;

}
