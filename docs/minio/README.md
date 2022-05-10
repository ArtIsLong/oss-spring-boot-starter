## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.3.5</version>
</dependency>
<!-- 解决：Unsupported OkHttp library found. Must use okhttp >= 4.8.1 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.9.3</version>
</dependency>
```

通过`oss.minio.enable=true`开启Minio OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个Minio OSS对象存储配置

```yaml
oss:
  minio:
    enable: true
    endpoint: https://play.min.io
    access-key: Q3AM3UQ867SPQQA43P2F
    secret-key: zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启Minio OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(MinioOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个Minio OSS对象存储配置

```yaml
oss:
  minio:
    enable: true
    oss-config:
      minioOssClient1:
     	endpoint: https://play.min.io
    	access-key: Q3AM3UQ867SPQQA43P2F
    	secret-key: zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG
    	bucket-name: Bucket1
    	base-path: /
    	slice-config:
      	  task-num: 8
      	  part-size: 104857600
      minioOssClient2:
     	endpoint: https://play.min.io
    	access-key: Q3AM3UQ867SPQQA43P2F
    	secret-key: zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG
    	bucket-name: Bucket2
    	base-path: /
    	slice-config:
      	  task-num: 8
      	  part-size: 104857600
```

当配置多个Minio OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("minioOssClient1")
private StandardOssClient minioOssClient1;
@Autowired
@Qualifier("minioOssClient2")
private StandardOssClient minioOssClient2;
```

- 客户端自定义配置

可通过oss.minio.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.minio.model.MinioOssClientConfig`类查看。

![image-20220504202147332](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220504202147332.png)

示例如下：

```yaml
oss:
  minio:
    enable: true
    endpoint: https://play.min.io
    access-key: Q3AM3UQ867SPQQA43P2F
    secret-key: zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
    client-config:
      connect-timeout: 5
      write-timeout: 5
      read-timeout: 5
      # ......
```

- 配置复用

当使用同一个Minio OSS对象存储多个不同Bucket时，可复用endpoint、accessKey、secretKey，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  minio:
    enable: true
    endpoint: https://play.min.io
    access-key: Q3AM3UQ867SPQQA43P2F
    secret-key: zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG
    slice-config:
      task-num: 8
      part-size: 104857600
	oss-config:
      minioOssClient1:
        bucket-name: Bucket1
      minioOssClient2:
        bucket-name: Bucket2
```

