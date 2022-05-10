## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.huaweicloud</groupId>
    <artifactId>esdk-obs-java-bundle</artifactId>
    <version>3.21.11</version>
</dependency>
```

通过`oss.huawei.enable=true`开启华为云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个华为云OSS对象存储配置

```yaml
oss:
  huawei:
    enable: true
    access-key: AccessKey
    secret-key: SecretKey
    end-point: EndPoint
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启华为云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(BaiduOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个华为云OSS对象存储配置

```yaml
oss:
  huawei:
    enable: true
    oss-config:
      huaweiOssClient1:
        access-key: AccessKey
	    secret-key: SecretKey
	    end-point: EndPoint
    	bucket-name: Bucket
	    base-path: /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      huaweiOssClient2:
        access-key: AccessKey
	    secret-key: SecretKey
	    end-point: EndPoint
    	bucket-name: Bucket
	    base-path: /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个华为云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("huaweiOssClient1")
private StandardOssClient huaweiOssClient1;
@Autowired
@Qualifier("huaweiOssClient2")
private StandardOssClient huaweiOssClient2;
```

- 客户端自定义配置

可通过oss.huawei.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.huawei.model.HuaWeiOssClientConfig`类查看。

![image-20220428162854964](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220428162854964.png)

示例如下：

```yaml
oss:
  huawei:
    enable: true
    access-key: AccessKey
    secret-key: SecretKey
    end-point: EndPoint
    bucket-name: Bucket
    base-path: /
    client-config:
      connection-timeout: 50000
      max-connections: 50
      # ...... 
```

- 配置复用

当使用同一个华为云OSS对象存储多个不同Bucket时，可复用accessKey、secretKey，endPoint、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  huawei:
    enable: true
    access-key: AccessKey
    secret-key: SecretKey
    end-point: EndPoint
    slice-config:
      task-num: 8
      part-size: 104857600
	oss-config:
      huaweiOssClient1:
        bucket-name: Bucket1
      huaweiOssClient2:
        bucket-name: Bucket2
```

