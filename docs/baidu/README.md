## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.baidubce</groupId>
    <artifactId>bce-java-sdk</artifactId>
    <version>0.10.196</version>
</dependency>
```

通过`oss.baidu.enable=true`开启百度云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个百度云OSS对象存储配置

```yaml
oss:
  baidu:
    enable: true
    access-key-id: AccessKeyId
    secret-access-key: SecretAccessKey
    bucket-name: Bucket
    base-path: /  # 存储根路径，默认路径为 /
    slice-config:
      task-num: 8  # 并发线程数,默认等于CPU的核数
      part-size: 104857600  # 分片大小,单位KB,默认5MB
```

通过如上配置即可开启百度云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(BaiduOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个百度云OSS对象存储配置

```yaml
oss:
  baidu:
    enable: true
    oss-config:
      baiduOssClient1:
        access-key-id: AccessKeyId1
        secret-access-key: SecretAccessKey1
        bucket-name: Bucket1
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      baiduOssClient2:
        access-key-id: AccessKeyId2
        secret-access-key: SecretAccessKey2
        bucket-name: Bucket2
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个百度云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("baiduOssClient1")
private StandardOssClient baiduOssClient1;
@Autowired
@Qualifier("baiduOssClient2")
private StandardOssClient baiduOssClient2;
```

- 客户端自定义配置

可通过oss.baidu.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.baidu.model.BaiduOssClientConfig`类查看。

![image-20220425164933213](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220425164933213.png)

示例如下：

```yaml
oss:
  baidu:
    enable: true
    access-key-id: AccessKeyId
    secret-access-key: SecretAccessKey
    bucket-name: Bucket
    base-path: /  # 存储根路径，默认路径为 /
    client-config:
      connection-timeout-in-millis: 50000
      max-connections: 50
      # ...... 
```

- 配置复用

当使用同一个百度云OSS对象存储多个不同Bucket时，可复用accessKeyId、secretAccessKey、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  baidu:
    enable: true
    access-key-id: AccessKeyId
    secret-access-key: SecretAccessKey
    client-config:
      connection-timeout-in-millis: 50000
      max-connections: 50
      # ...... 
    oss-config:
      baiduOssClient1:
        bucket-name: Bucket1
      baiduOssClient2:
        bucket-name: Bucket2
```

