## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-java-sdk</artifactId>
    <version>7.9.3</version>
</dependency>
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>happy-dns-java</artifactId>
    <version>0.1.6</version>
</dependency>
```

通过`oss.qiniu.enable=true`开启七牛云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个七牛云OSS对象存储配置

```yaml
oss:
  qiniu:
  qiniu:
    enable: true
    access-key: AccessKey
    secret-key: SecretKey
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启七牛云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(QiniuOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个七牛云OSS对象存储配置

```yaml
oss:
  qiniu:
    enable: true
    oss-config:
      qiniuOssClient1:
        access-key: AccessKey
    	secret-key: SecretKey
    	bucket-name: Bucket
    	base-path: /
    	slice-config:
      	  task-num: 8
      	  part-size: 104857600
      qiniuOssClient2:
        access-key: AccessKey
    	secret-key: SecretKey
    	bucket-name: Bucket
    	base-path: /
    	slice-config:
      	  task-num: 8
      	  part-size: 104857600
```

当配置多个七牛云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("qiniuOssClient1")
private StandardOssClient qiniuOssClient1;
@Autowired
@Qualifier("qiniuOssClient2")
private StandardOssClient qiniuOssClient2;
```

- 客户端自定义配置

可通过oss.qiniu.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.qiniu.model.QiniuOssClientConfig`类查看。

![image-20220509000955142](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220509000955142.png)

示例如下：

```yaml
oss:
  qiniu:
    enable: true
    access-key: AccessKey
    secret-key: SecretKey
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
    client-config:
      connect-timeout: 60
      write-timeout: 100
      read-timeout: 100
      # ......
```

- 配置复用

当使用同一个七牛云OSS对象存储多个不同Bucket时，可复用accessKey、secretKey，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  qiniu:
    enable: true
    access-key: AccessKey
    secret-key: SecretKey
    slice-config:
      task-num: 8
      part-size: 104857600
	oss-config:
      qiniuOssClient1:
        bucket-name: Bucket1
      qiniuOssClient2:
        bucket-name: Bucket2
```

