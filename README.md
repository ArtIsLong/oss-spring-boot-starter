# OSS对象存储

本工具集成了常用的第三方对象存储平台，简化项目中使用对象存储时繁琐的集成过程，并针对实际使用过程中的积累，对常用方法进行封装，提供了一套标准的API，让对象存储开箱即用。

- 源码地址

[Github](https://github.com/ArtIsLong/oss-spring-boot-starter)

[Gitee](https://gitee.com/spring-boot-starter/oss-spring-boot-starter)

- 目前已支持：

[本地存储](https://hutool.cn/docs/#/core/IO/%E6%96%87%E4%BB%B6%E5%B7%A5%E5%85%B7%E7%B1%BB-FileUtil)

[FTP](https://commons.apache.org/proper/commons-net/download_net.cgi)

[SFTP](http://epaul.github.io/jsch-documentation/javadoc/com/jcraft/jsch/package-summary.html)

[阿里云](https://help.aliyun.com/product/31815.html)

[百度云](https://cloud.baidu.com/doc/BOS/index.html)

[华为云](https://support.huaweicloud.com/obs/index.html)

[京东云](https://docs.jdcloud.com/cn/object-storage-service/api/introduction-2)

[七牛云](https://developer.qiniu.com/kodo)

[腾讯云](https://cloud.tencent.com/document/product/436)

[又拍云](https://help.upyun.com/docs/storage/)

[Minio](http://www.minio.org.cn/)

[金山云](https://docs.ksyun.com/documents/38731)

特别说明：本地存储、SFTP、FTP三种实现方式主要基于[hutool](https://hutool.cn/)提供的`FileUtil`、`FileNameUtil`、`AbstractFtp`相关的工具。

## 开始使用

jar包已推送至maven中央仓库，可通过如下方式导入oss-spring-boot-starter依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
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

### 本地存储

当不使用第三方服务或存储平台，仅使用本机存储时，不需要导入额外的依赖包，在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  local:
    enable: true  # 开启本地存储
    base-path: /Study # 本地文件存储根路径
    slice-config:
      task-num: 8  # 并发线程数,默认等于CPU的核数
      part-size: 104857600 # 1024*1024*100 断点续传分片大小
```

- 批量配置

```yaml
oss:
  local:
    enable: true  # 开启本地存储
    oss-config:
      local1:
        base-path: /Study1
        slice-config:
        task-num: 8  # 并发线程数,默认等于CPU的核数
        part-size: 104857600  # 1024*1024*100 断点续传分片大小
```

### FTP

```xml
<dependency>
    <groupId>commons-net</groupId>
    <artifactId>commons-net</artifactId>
    <version>3.8.0</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  ftp:
    enable: true
    host: FTP服务器IP
    port: FTP服务端口
    user: 用户名
    password: 密码
    charset: 编码
    base-path: FTP服务器存储根路径
```

- 批量配置

```yaml
oss:
  ftp:
    enable: true
    oss-config:
      ftp1:
        host: FTP服务器IP
        port: FTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: FTP服务器存储根路径
```

### SFTP

```xml
<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
<dependency>
    <groupId>commons-net</groupId>
    <artifactId>commons-net</artifactId>
    <version>3.8.0</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  sftp:
    enable: true
    host: SFTP服务器IP
    port: SFTP服务端口
    user: 用户名
    password: 密码
    charset: 编码
    base-path: FTP服务器存储根路径
```

- 批量配置

```yaml
oss:
  sftp:
    enable: true
    oss-config:
      sftp1:
        host: SFTP服务器IP
        port: SFTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: FTP服务器存储根路径
```

### 阿里云

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.13.2</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  ali:
    enable: true
    access-key-id: accessKeyId
    access-key-secret: accessKeySecret
    endpoint: endpoint
    bucket-name: bucketName
    base-path: 阿里云存储根路径
    slice-config:
      task-num: 8
      part-size: 104857600 # 1024*1024*100
```

- 批量配置

```yaml
oss:
  ali:
    enable: true
    oss-config:
      aliOssClient1:
        access-key-id: accessKeyId
        access-key-secret: accessKeySecret
        endpoint: endpoint
        bucket-name: bucketName
        base-path: 阿里云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

**注：** 对于批量配置，如access-key-id、access-key-secret、endpoint可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  ali:
    enable: true
    access-key-id: accessKeyId
    access-key-secret: accessKeySecret
    endpoint: endpoint
    oss-config:
      aliOssClient1:
        bucket-name: bucketName
        base-path: 阿里云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

### 华为云

```xml
<dependency>
    <groupId>com.huaweicloud</groupId>
    <artifactId>esdk-obs-java-bundle</artifactId>
    <version>3.21.8.1</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  huawei:
    enable: true
    endpoint: endpoint
    access-key: accessKey
    secret-key: secretKey
    bucket-name: backetName
    base-path: 华为云存储根路径
    slice-config:
      task-num: 8
      part-size: 104857600 # 1024*1024*100
```

- 批量配置

```yaml
oss:
  huawei:
    enable: true
    oss-config:
      huaweiOssClient1:
        endpoint: endpoint
        access-key: accessKey
        secret-key: secretKey
        bucket-name: backetName
        base-path: 华为云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

**注：** 对于批量配置，如access-key、secret-key、endpoint可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  huawei:
    enable: true
    endpoint: endpoint
    access-key: accessKey
    secret-key: secretKey
    oss-config:
      huaweiOssClient1:
        bucket-name: backetName
        base-path: 华为云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

### 京东云

```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
    <version>1.12.117</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  jd:
    enable: true
    endpoint: endpoint
    region: region
    access-key: accessKey
    secret-key: secretKey
    bucket-name: bucket
    base-path: 京东云存储根路径
    slice-config:
      task-num: 8
      part-size: 104857600 # 1024*1024*100
```

- 批量配置

```yaml
oss:
  jd:
    enable: true
    oss-config:
      jdOssClient1:
        endpoint: endpoint
        region: region
        access-key: accessKey
        secret-key: secretKey
        bucket-name: bucket
        base-path: 京东云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

**注：** 对于批量配置，如access-key、secret-key、endpoint、region可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  jd:
    enable: true
    endpoint: endpoint
    region: region
    access-key: accessKey
    secret-key: secretKey
    oss-config:
      jdOssClient1:
        bucket-name: bucket
        base-path: 京东云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

### 七牛云

```xml
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-java-sdk</artifactId>
    <version>7.8.0</version>
</dependency>
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>happy-dns-java</artifactId>
    <version>0.1.6</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  qiniu:
    enable: true
    region: region
    access-key: accessKey
    secret-key: secretKey
    bucket-name: bucket
    base-path: 七牛云存储根路径
    slice-config:
      task-num: 8
      part-size: 104857600 # 1024*1024*100
```

- 批量配置

```yaml
oss:
  qiniu:
    enable: true
    oss-config:
      qiNiuOssClient1:
        region: region
        access-key: accessKey
        secret-key: secretKey
        bucket-name: bucket
        base-path: 七牛云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

**注：** 对于批量配置，如region、access-key、secret-key可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  qiniu:
    enable: true
    region: region
    access-key: accessKey
    secret-key: secretKey
    oss-config:
      qiNiuOssClient1:
        bucket-name: bucket
        base-path: 七牛云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

### 腾讯云

```xml
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
    <version>5.6.61</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  tencent:
    enable: true
    region: region
    secret-key: secretKey
    secret-id: secretId
    bucket-name: bucket
    base-path: 腾讯云存储根路径
    slice-config:
      task-num: 8
      part-size: 104857600 # 1024*1024*100
```

- 批量配置

```yaml
oss:
  tencent:
    enable: true
    oss-config:
      tencentOssClient1:
        region: region
        secret-key: secretKey
        secret-id: secretId
        bucket-name: bucket
        base-path: 腾讯云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

**注：** 对于批量配置，如region、secret-key、secret-id可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  tencent:
    enable: true
    region: region
    secret-key: secretKey
    secret-id: secretId
    oss-config:
      tencentOssClient1:
        bucket-name: bucket
        base-path: 腾讯云存储根路径
        slice-config:
          task-num: 8
          part-size: 104857600 # 1024*1024*100
```

### 又拍云

```xml
<dependency>
    <groupId>com.upyun</groupId>
    <artifactId>java-sdk</artifactId>
    <version>4.2.3</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  up:
    enable: true
    user-name: 用户名
    password: 密码
    bucket-name: bucketName
    base-path: 又拍云存储根路径
    slice-config:
      task-num: 8
```

- 批量配置

```yaml
oss:
  up:
    enable: true
    oss-config:
      upOssClient1:
        user-name: 用户名
        password: 密码
        bucket-name: bucketName
        base-path: 又拍云存储根路径
        slice-config:
          task-num: 8
```

**注：** 对于批量配置，如user-name、password可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  up:
    enable: true
    user-name: 用户名
    password: 密码
    oss-config:
      upOssClient1:
        bucket-name: bucketName
        base-path: 又拍云存储根路径
        slice-config:
          task-num: 8
```

### Minio

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.3.4</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.9.3</version>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  minio:
    enable: true
    endpoint: 地址
    access-key: 用户名
    secret-key: 密码
    bucket-name: bucketName
    base-path: Minio存储根路径
```

- 批量配置

```yaml
oss:
  minio:
    enable: true
    oss-config:
      minioOssClient1:
        endpoint: 地址
        access-key: 用户名
        secret-key: 密码
        bucket-name: bucketName
        base-path: Minio存储根路径
```

**注：** 对于批量配置，如endpoint、access-key、secret-key可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  minio:
    enable: true
    endpoint: 地址
    access-key: 用户名
    secret-key: 密码
    oss-config:
      minioOssClient1:
        bucket-name: bucketName
        base-path: Minio存储根路径
```

### 金山云

```xml
<dependency>
    <groupId>com.ksyun</groupId>
    <artifactId>ks3-kss-java-sdk</artifactId>
    <version>1.0.2</version>
    <exclusions>
        <exclusion>
            <artifactId>commons-logging</artifactId>
            <groupId>commons-logging</groupId>
        </exclusion>
    </exclusions>
</dependency>
```

在application.yml中增加如下配置：

- 单个配置

```yaml
oss:
  jinshan:
    enable: true
    endpoint: Endpoint
    access-key-id: accessKeyId
    access-key-secret: accessKeySecret
    bucket-name: bucket
    base-path: 根路径
    slice-config:
      task-num: 8
      part-size: 104857600
```

- 批量配置

```yaml
oss:
  jinshan:
    enable: true
    oss-config:
      jinshanOssClient1:
        endpoint: Endpoint
        access-key-id: accessKeyId
        access-key-secret: accessKeySecret
        bucket-name: bucket
        base-path: 根路径
        slice-config:
          task-num: 8
          part-size: 104857600
```

**注：** 对于批量配置，如endpoint、access-key、secret-key可复用，基础配置中配置这三个参数，批量配置中配置其他如bucket-name等参数即可，示例如下：

```yaml
oss:
  jinshan:
    enable: true
    endpoint: Endpoint
    access-key-id: accessKeyId
    access-key-secret: accessKeySecret
    oss-config:
      jinshanOssClient1:
        bucket-name: bucket
        base-path: 根路径
        slice-config:
          task-num: 8
          part-size: 104857600
```

新功能持续增加中，敬请期待！！！

## 单元测试

![](http://assets.processon.com/chart_image/620774561e08530f01793ea6.png)

