## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.ksyun</groupId>
    <artifactId>ks3-kss-java-sdk</artifactId>
    <version>1.0.2</version>
</dependency>
```

通过`oss.jinshan.enable=true`开启金山云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个金山云OSS对象存储配置

```yaml
oss:
  jinshan:
    enable: true
    access-key-id: AccessKeyId
    access-key-secret: AccessKeySecret
    endpoint: Endpoint
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启金山云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(JinShanOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个金山云OSS对象存储配置

```yaml
oss:
  jinshan:
    enable: true
    oss-config:
      jinshanOssClient1:
        access-key-id: AccessKeyId
    	access-key-secret: AccessKeySecret
    	endpoint: Endpoint
    	bucket-name: Bucket
	    base-path: /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      jinshanOssClient2:
        access-key-id: AccessKeyId
    	access-key-secret: AccessKeySecret
    	endpoint: Endpoint
    	bucket-name: Bucket
	    base-path: /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个金山云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("jinshanOssClient1")
private StandardOssClient jinshanOssClient1;
@Autowired
@Qualifier("jinshanOssClient2")
private StandardOssClient jinshanOssClient2;
```

- 客户端自定义配置

可通过oss.jinshan.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.jinshan.model.JinShanOssClientConfig`类查看。

![image-20220502223144158](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220502223144158.png)

示例如下：

```yaml
oss:
  jinshan:
    enable: true
    access-key-id: AccessKeyId
    access-key-secret: AccessKeySecret
    endpoint: Endpoint
    bucket-name: Bucket
    base-path: /
    client-config:
      connection-time-out: 50000
      max-connections: 50
      # ...... 
```

- 配置复用

当使用同一个金山云OSS对象存储多个不同Bucket时，可复用accessKeyId、accessKeySecret、endpoint、region、clientConfig、securityToken，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  jinshan:
    enable: true
    access-key-id: AccessKeyId
    access-key-secret: AccessKeySecret
    endpoint: Endpoint
    slice-config:
      task-num: 8
      part-size: 104857600
	oss-config:
      jinshanOssClient1:
        bucket-name: Bucket1
      jinshanOssClient2:
        bucket-name: Bucket2
```

