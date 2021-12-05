package com.github.constant;

/**
 * @author 陈敏
 * @version OssConstant.java, v 1.1 2021/11/5 11:06 chenmin Exp $
 * Created on 2021/11/5
 */
public class OssConstant {

    public static final String OSS = "oss";

    /**
     * OSS存储类型
     */
    public interface OssType {
        String LOCAL = "local";
        String FTP = "ftp";
        String SFTP = "sftp";
        String ALI = "ali";
        String QINIU = "qiniu";
        String MINIO = "minio";
        String BAIDU = "baidu";
        String TENCENT = "tencent";
        String HUAWEI = "huawei";
        String JD = "jd";
        String UP = "up";
    }

}
