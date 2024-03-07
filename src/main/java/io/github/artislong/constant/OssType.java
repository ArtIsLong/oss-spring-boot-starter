package io.github.artislong.constant;

/**
 * OSS存储类型
 * @author 陈敏
 * @version OssType.java, v 1.1 2021/11/16 21:54 chenmin Exp $
 * Created on 2021/11/16
 */
public class OssType {

    public static final String LOCAL = "local";
    public static final String FTP = "ftp";
    public static final String SFTP = "sftp";
    public static final String ALI = "ali";
    public static final String QINIU = "qiniu";
    public static final String MINIO = "minio";
    public static final String BAIDU = "baidu";
    public static final String TENCENT = "tencent";
    public static final String HUAWEI = "huawei";
    public static final String JD = "jd";
    public static final String UP = "up";
    public static final String JINSHAN = "jinshan";
    public static final String WANGYI = "wangyi";
    public static final String UCLOUD = "ucloud";
    public static final String PINGAN = "pingan";
    public static final String QINGYUN = "qingyun";
    public static final String JDBC = "jdbc";
    public static final String AWS = "aws";
    public static final String CTYUN = "ctyun";
    public static final String ECLOUD = "cloud";
    public static final String INSPUR = "inspur";
    public static final String FDFS = "fdfs";

    public enum Enums {
        /**
         * 本地磁盘存储
         */
        LOCAL(OssType.LOCAL),

        /**
         * FTP协议存储
         */
        FTP(OssType.FTP),

        /**
         * SFTP存储
         */
        SFTP(OssType.SFTP),

        /**
         * 阿里OSS存储
         */
        ALI(OssType.ALI),

        /**
         * 七牛云存储
         */
        QINIU(OssType.QINIU),

        /**
         * MinIO存储
         */
        MINIO(OssType.MINIO),

        /**
         * 百度云存储
         */
        BAIDU(OssType.BAIDU),

        /**
         * 腾讯云存储
         */
        TENCENT(OssType.TENCENT),

        /**
         * 华为云存储
         */
        HUAWEI(OssType.HUAWEI),

        /**
         * 京东云存储
         */
        JD(OssType.JD),

        /**
         * 又拍云存储
         */
        UP(OssType.UP),

        /**
         * 金山云
         */
        JINSHAN(OssType.JINSHAN),

        /**
         * 网易数帆
         */
        WANGYI(OssType.WANGYI),

        /**
         * UCloud
         */
        UCLOUD(OssType.UCLOUD),

        /**
         * 平安云
         */
        PINGAN(OssType.PINGAN),

        /**
         * 青云
         */
        QINGYUN(OssType.QINGYUN),

        /**
         * JDBC
         */
        JDBC(OssType.JDBC),

        /**
         * 亚马逊
         */
        AWS(OssType.AWS),

        /**
         * 天翼云
         */
        CTYUN(OssType.CTYUN),

        /**
         * 移动云
         */
        ECLOUD(OssType.ECLOUD),

        /**
         * 浪潮云
         */
        INSPUR(OssType.INSPUR),

        /**
         * FDFS
         */
        FDFS(OssType.FDFS);

        private final String value;

        Enums(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
