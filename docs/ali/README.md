## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.13.2</version>
</dependency>
```

通过`oss.ali.enable=true`开启阿里云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个阿里云OSS对象存储配置

```yaml
oss:
  ali:
    enable: true
    access-key-id: AccessKeyId
    access-key-secret: AccessKeySecret
    endpoint: Endpoint
    bucket-name: Bucket
    base-path: /  # 存储根路径，默认路径为 /
    slice-config:
      task-num: 8  # 并发线程数,默认等于CPU的核数
      part-size: 104857600  # 分片大小,单位KB,默认5MB
```

通过如上配置即可开启阿里云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(AliOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个阿里云OSS对象存储配置

```yaml
oss:
  ali:
    enable: true
    oss-config:
      aliOssClient1:
        access-key-id: AccessKeyId1
        access-key-secret: AccessKeySecret1
        endpoint: Endpoint1
        bucket-name: Bucket1
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      aliOssClient2:
        access-key-id: AccessKeyId2
        access-key-secret: AccessKeySecret2
        endpoint: Endpoint2
        bucket-name: Bucket2
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个阿里云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("aliOssClient1")
private StandardOssClient aliOssClient1;
@Autowired
@Qualifier("aliOssClient2")
private StandardOssClient aliOssClient2;
```

- 客户端自定义配置

可通过oss.ali.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.ali.model.AliOssClientConfig`类查看。

![image-20220425152135392](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220425152135392.png)

示例如下：

```yaml
oss:
  ali:
    enable: true
    access-key-id: AccessKeyId
    access-key-secret: AccessKeySecret
    endpoint: Endpoint
    bucket-name: Bucket
    client-config:
      socket-timeout: 50000
      connection-timeout: 50000
      # ...... 
```

- 配置复用

当使用同一个阿里云对象存储多个不同Bucket时，可复用endpoint、accessKeyId、accessKeySecret、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  ali:
    enable: true
    access-key-id: AccessKeyId
    access-key-secret: AccessKeySecret
    endpoint: Endpoint
    oss-config:
      aliOssClient1:
        bucket-name: Bucket1
      aliOssClient2:
        bucket-name: Bucket2
```

