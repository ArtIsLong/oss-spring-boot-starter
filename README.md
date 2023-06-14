# OSS对象存储

本工具集成了常用的第三方对象存储平台，简化项目中使用对象存储时繁琐的集成过程，并针对实际使用过程中的积累，对常用方法进行封装，提供了一套标准的API，让对象存储开箱即用。

- 源码地址

[Github](https://github.com/ArtIsLong/oss-spring-boot-starter)

[Gitee](https://gitee.com/spring-boot-starter/oss-spring-boot-starter)

- 目前已支持：

| 序号 | 存储器   | 官方地址                                                     | 说明文档                       |
| ---- | -------- | ------------------------------------------------------------ | ------------------------------ |
| 1    | 本地存储 | [点击](https://hutool.cn/docs/#/core/IO/%E6%96%87%E4%BB%B6%E5%B7%A5%E5%85%B7%E7%B1%BB-FileUtil) | [点击](docs/local/README.md)   |
| 2    | FTP      | [点击](https://commons.apache.org/proper/commons-net/download_net.cgi) | [点击](docs/ftp/README.md)     |
| 3    | SFTP     | [点击](http://epaul.github.io/jsch-documentation/javadoc/com/jcraft/jsch/package-summary.html) | [点击](docs/sftp/README.md)    |
| 4    | 阿里云   | [点击](https://help.aliyun.com/product/31815.html)           | [点击](docs/ali/README.md)     |
| 5    | 百度云   | [点击](https://cloud.baidu.com/doc/BOS/index.html)           | [点击](docs/baidu/README.md)   |
| 6    | 华为云   | [点击](https://support.huaweicloud.com/obs/index.html)       | [点击](docs/huawei/README.md)  |
| 7    | 京东云   | [点击](https://docs.jdcloud.com/cn/object-storage-service/api/introduction-2) | [点击](docs/jd/README.md)      |
| 8    | 七牛云   | [点击](https://developer.qiniu.com/kodo)                     | [点击](docs/qiniu/README.md)   |
| 9    | 腾讯云   | [点击](https://cloud.tencent.com/document/product/436)       | [点击](docs/tencent/README.md) |
| 10   | Minio    | [点击](http://www.minio.org.cn/)                             | [点击](docs/minio/README.md)   |
| 11   | 又拍云   | [点击](https://help.upyun.com/docs/storage/)                 | [点击](docs/up/README.md)      |
| 12   | 金山云   | [点击](https://docs.ksyun.com/documents/38731)               | [点击](docs/jinshan/README.md) |
| 13   | JDBC     | [点击](docs/jdbc/README.md)                                  | [点击](docs/jdbc/README.md)    |
| 14   | 青云     | [点击](https://docsv3.qingcloud.com/storage/object-storage/intro/object-storage/) | [点击](docs/qingyun/README.md) |
| 15   | 网易数帆 | [点击](https://sf.163.com/help/documents/68792520222625792)  | [点击](docs/wangyi/README.md)  |
| 16   | 亚马逊   | [点击](https://docs.aws.amazon.com/s3/)                      | [点击](docs/aws/README.md)     |
| 17   | UCloud   | [点击](https://docs.ucloud.cn/ufile/README)                  | [点击](docs/ucloud/README.md)  |
| 18   | 平安云   | [点击](https://yun.pingan.com/ssr/help/storage/obs/OBS_SDK_.Java_SDK_) | [点击](docs/pingan/README.md)  |

新功能持续增加中，敬请期待！！！

**特别说明**：本地存储、SFTP、FTP三种实现方式主要基于[hutool](https://hutool.cn/)提供的`FileUtil`、`Sftp`、`Ftp`等相关的工具。

## 开始使用

jar包已推送至maven中央仓库，可通过如下方式导入oss-spring-boot-starter依赖

```xml
<dependency>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependency>
```

在需要使用的Spring Bean中注入`StandardOssClient`对象即可。

StandardOssClient类提供统一的文件存储API，提供了如下方法：

- 文件上传

```java
/**
 * 上传文件，默认覆盖
 * @param is 输入流
 * @param targetName 目标文件路径
 * @return 返回文件路径
 */
default OssInfo upLoad(InputStream is,String targetName) {
    return upLoad(is,targetName,true);
}
/**
 * 上传文件
 * @param is 输入流
 * @param targetName 目标文件路径
 * @param isOverride 是否覆盖
 * @return 返回文件路径
 */
OssInfo upLoad(InputStream is,String targetName,Boolean isOverride);
```

- 断点续传上传

```java
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
```

- 文件下载

```java
/**
 * 下载文件
 * @param os  输出流
 * @param targetName  目标文件路径
 */
void downLoad(OutputStream os,String targetName);
```

- 断点续传下载

```java
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
void downLoadCheckPoint(File localFile, String targetName);
```

- 删除

```java
/**
 * 删除文件
 * @param targetName 目标文件路径
 */
void delete(String targetName);
```

- 复制

```java
/**
 * 复制文件，默认覆盖
 * @param sourceName 源文件路径
 * @param targetName 目标文件路径
 */
default void copy(String sourceName,String targetName) {
    copy(sourceName,targetName,true);
}
/**
 * 复制文件
 * @param sourceName 源文件路径
 * @param targetName 目标文件路径
 * @param isOverride 是否覆盖
 */
void copy(String sourceName,String targetName,Boolean isOverride);
```

- 移动

```java
/**
 * 移动文件，默认覆盖
 * @param sourceName 源文件路径
 * @param targetName 目标路径
 */
default void move(String sourceName,String targetName) {
    move(sourceName,targetName,true);
}
/**
 * 移动文件
 * @param sourceName 源文件路径
 * @param targetName 目标路径
 * @param isOverride 是否覆盖
 */
void move(String sourceName,String targetName,Boolean isOverride);
```

- 重命名

```java
/**
 * 重命名文件
 * @param sourceName 源文件路径
 * @param targetName 目标文件路径
 */
default void rename(String sourceName,String targetName) {
    rename(sourceName,targetName,true);
}
/**
 * 重命名文件
 * @param sourceName 源文件路径
 * @param targetName 目标路径
 * @param isOverride 是否覆盖
 */
void rename(String sourceName,String targetName,Boolean isOverride);
```

- 获取文件及目录信息

```java
/**
 * 获取文件信息，默认获取目标文件信息
 * @param targetName 目标文件路径
 * @return 文件基本信息
 */
default OssInfo getInfo(String targetName) {
    return getInfo(targetName,false);
}
/**
 * 获取文件信息
 *      isRecursion传false，则只获取当前对象信息；
 *      isRecursion传true，且当前对象为目录时，会递归获取当前路径下所有文件及目录，按层级返回
 * @param targetName 目标文件路径
 * @param isRecursion 是否递归
 * @return 文件基本信息
 */
OssInfo getInfo(String targetName,Boolean isRecursion);
```

- 判断对象是否为文件

```java
/**
 * 是否为文件
 *      默认根据路径最后一段名称是否有后缀名来判断是否为文件，此方式不准确，当存储平台不提供类似方法时，可使用此方法
 * @param targetName 目标文件路径
 * @return true/false
 */
default Boolean isFile(String targetName) {
    String name=FileNameUtil.getName(targetName);
    return StrUtil.indexOf(name,StrUtil.C_DOT)>0;
}
```

- 判断对象是否为目录

```java
/**
 * 是否为目录
 *      与判断是否为文件相反
 * @param targetName 目标文件路径
 * @return true/false
 */
default Boolean isDirectory(String targetName) {
    return !isFile(targetName);
}
```

- 判断对象是否存在

```java
/**
 * 是否存在
 * @param targetName 目标文件路径
 * @return true/false
 */
Boolean isExist(String targetName);
```

- 分片上传

```java
/**
 * 上传分片
 * @param upLoadCheckPoint 断点续传对象
 * @param partNum 分片索引
 * @return 上传结果
 */
UpLoadPartResult uploadPart(UpLoadCheckPoint upLoadCheckPoint, int partNum, InputStream inputStream);
```

- 分片下载

```java
/**
 * 下载分片
 * @param key 目标文件
 * @param start 文件开始字节
 * @param end 文件结束字节
 * @return 此范围的文件流
 */
InputStream downloadPart(String key, long start, long end);
```

更多API可通过[在线API文档](https://apidoc.gitee.com/spring-boot-starter/oss-spring-boot-starter/)查看。

具体使用可根据实际业务需求及所采用的存储平台灵活使用。

**注意：** 在开启多个存储平台后，在注入操作客户端时，需通过`@Qualifier`注解指定Bean名称，同时，每个存储平台配置多实例时，将按照自定义名称注入。具体注入方式可通过test包路径中查看。

## 单元测试

![](http://assets.processon.com/chart_image/620774561e08530f01793ea6.png)
