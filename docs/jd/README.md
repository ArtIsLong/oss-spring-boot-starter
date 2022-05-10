## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
    <version>1.12.167</version>
</dependency>
```

通过`oss.jd.enable=true`开启京东云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个京东云OSS对象存储配置

```yaml
oss:
  jd:
    enable: true
    endpoint: Endpoint
    region: Region
    access-key: AccessKey
    secret-key: SecretKey
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启京东云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(JdOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个京东云OSS对象存储配置

```yaml
oss:
  jd:
    enable: true
    oss-config:
      jdOssClient1:
        endpoint: Endpoint
    	region: Region
    	access-key: AccessKey
    	secret-key: SecretKey
    	bucket-name: Bucket
	    base-path: /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      jdOssClient2:
        endpoint: Endpoint
    	region: Region
    	access-key: AccessKey
    	secret-key: SecretKey
    	bucket-name: Bucket
	    base-path: /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个京东云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("jdOssClient1")
private StandardOssClient jdOssClient1;
@Autowired
@Qualifier("jdOssClient2")
private StandardOssClient jdOssClient2;
```

- 客户端自定义配置

可通过oss.jd.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.jd.model.JdOssClientConfig`类查看。

![image-20220510125807151](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220510125807151.png)

示例如下：

```yaml
oss:
  jd:
    enable: true
    endpoint: Endpoint
    region: Region
    access-key: AccessKey
    secret-key: SecretKey
    bucket-name: Bucket
    base-path: /
    client-config:
      connection-timeout: 50000
      max-connections: 50
      # ...... 
```

- 配置复用

当使用同一个京东云OSS对象存储多个不同Bucket时，可复用endpoint、region、accessKey，secretKey、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  jd:
    enable: true
    endpoint: Endpoint
    region: Region
    access-key: AccessKey
    secret-key: SecretKey
    slice-config:
      task-num: 8
      part-size: 104857600
    client-config:
      connection-timeout: 50000
      max-connections: 50
      # ......    
	oss-config:
      jdOssClient1:
        bucket-name: Bucket1
      jdOssClient2:
        bucket-name: Bucket2
```

