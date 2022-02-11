package io.github.artislong.constant;

/**
 * @author 陈敏
 * @version OssConstant.java, v 1.1 2021/11/5 11:06 chenmin Exp $
 * Created on 2021/11/5
 */
public class OssConstant {

    public static final String OSS = "oss";

    /**
     * 默认分片大小
     */
    public static final Long DEFAULT_PART_SIZE = 1024 * 1024 * 5L;
    /**
     * 默认并发线程数
     */
    public static final Integer DEFAULT_TASK_NUM = Runtime.getRuntime().availableProcessors();

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
