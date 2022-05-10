## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.upyun</groupId>
    <artifactId>java-sdk</artifactId>
    <version>4.2.3</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.github.tobato</groupId>
    <artifactId>fastdfs-client</artifactId>
    <version>1.27.2</version>
    <scope>provided</scope>
</dependency>
```

通过`oss.up.enable=true`开启又拍云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个又拍云OSS对象存储配置

```yaml
oss:
  up:
    enable: true
    user-name: UserName
    password: Password
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启又拍云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(UpOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个又拍云OSS对象存储配置

```yaml
oss:
  up:
    enable: true
    oss-config:
      upOssClient1:
        user-name: UserName
	    password: Password
        bucket-name: Bucket1
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      upOssClient2:
        user-name: UserName
	    password: Password
        bucket-name: Bucket2
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个又拍云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("upOssClient1")
private StandardOssClient upOssClient1;
@Autowired
@Qualifier("upOssClient2")
private StandardOssClient upOssClient2;
```

- 客户端自定义配置

可通过oss.up.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.up.model.UpOssClientConfig`类查看。

![image-20220510003936079](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220510003936079.png)

示例如下：

```yaml
oss:
  up:
    enable: true
    user-name: UserName
    password: Password
    bucket-name: Bucket
    base-path: /  # 存储根路径，默认路径为 /
    client-config:
      timeout: 30
      api-domain: ED_AUTO
      # ...... 
```

- 配置复用

当使用同一个又拍云OSS对象存储多个不同Bucket时，可复用userName、password、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  up:
    enable: true
    user-name: UserName
    password: Password
    client-config:
      timeout-connect: 50000
      timeout-read: 50000
      # ...... 
    oss-config:
      upOssClient1:
        bucket-name: Bucket1
      upOssClient2:
        bucket-name: Bucket2
```

