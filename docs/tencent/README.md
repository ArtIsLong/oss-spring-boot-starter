## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
    <version>5.6.69</version>
</dependency>
```

通过`oss.tencent.enable=true`开启腾讯云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个腾讯云OSS对象存储配置

```yaml
oss:
  tencent:
    enable: true
    secret-key: SecretKey
    secret-id: SecretId
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
    client-config:
      region: cos.ap-chengdu
```

通过如上配置即可开启腾讯云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(TencentOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个腾讯云OSS对象存储配置

```yaml
oss:
  tencent:
    enable: true
    oss-config:
      tencentOssClient1:
        secret-key: SecretKey1
	    secret-id: SecretId1
        bucket-name: Bucket1
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
        client-config:
          region: cos.ap-chengdu
      tencentOssClient2:
        secret-key: SecretKey1
	    secret-id: SecretId1
        bucket-name: Bucket2
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
        client-config:
          region: cos.ap-chengdu
```

当配置多个腾讯云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("tencentOssClient1")
private StandardOssClient tencentOssClient1;
@Autowired
@Qualifier("tencentOssClient2")
private StandardOssClient tencentOssClient2;
```

- 客户端自定义配置

可通过oss.tencent.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.tencent.model.TencentOssClientConfig`类查看。

![image-20220510001523333](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220510001523333.png)

示例如下：

```yaml
oss:
  tencent:
    enable: true
    secret-key: SecretKey
    secret-id: SecretId
    bucket-name: Bucket
    base-path: /  # 存储根路径，默认路径为 /
    client-config:
      region: cos.ap-chengdu
      connection-timeout: 50000
      max-connections-count: 50
      # ...... 
```

- 配置复用

当使用同一个腾讯云OSS对象存储多个不同Bucket时，可复用secretKey、secretId、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  tencent:
    enable: true
    secret-key: SecretKey
    secret-id: SecretId
    client-config:
      region: cos.ap-chengdu
      connection-timeout: 50000
      max-connections-count: 50
      # ...... 
    oss-config:
      tencentOssClient1:
        bucket-name: Bucket1
      tencentOssClient2:
        bucket-name: Bucket2
```

