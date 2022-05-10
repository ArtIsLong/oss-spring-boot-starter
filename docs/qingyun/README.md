## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.yunify</groupId>
    <artifactId>qingstor.sdk.java</artifactId>
    <version>2.5.2</version>
</dependency>
```

通过`oss.qingyun.enable=true`开启青云OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个青云OSS对象存储配置

```yaml
oss:
  qingyun:
    enable: true
    zone: Zone
    access-key: AccessKey
    access-secret: AccessSecret
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启青云OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(QingYunOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个青云OSS对象存储配置

```yaml
oss:
  qingyun:
    enable: true
    oss-config:
      qingyunOssClient1:
        zone: Zone
        access-key: AccessKey
        access-secret: AccessSecret
    	bucket-name: Bucket1
    	base-path: /
    	slice-config:
      	  task-num: 8
      	  part-size: 104857600
      qingyunOssClient2:
        zone: Zone
        access-key: AccessKey
        access-secret: AccessSecret
    	bucket-name: Bucket2
    	base-path: /
    	slice-config:
      	  task-num: 8
      	  part-size: 104857600
```

当配置多个青云OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("qingyunOssClient1")
private StandardOssClient qingyunOssClient1;
@Autowired
@Qualifier("qingyunOssClient2")
private StandardOssClient qingyunOssClient2;
```

- 客户端自定义配置

可通过oss.qingyun.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.qingyun.model.QingYunOssClientConfig`类查看。

![image-20220508234832007](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220508234832007.png)

示例如下：

```yaml
oss:
  qingyun:
    enable: true
    zone: Zone
    access-key: AccessKey
    access-secret: AccessSecret
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
    client-config:
      connection-timeout: 60
      write-timeout: 100
      read-timeout: 100
      # ......
```

- 配置复用

当使用同一个青云OSS对象存储多个不同Bucket时，可复用zone、endpoint、accessKey、accessSecret，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  qingyun:
    enable: true
    zone: Zone
    access-key: AccessKey
    access-secret: AccessSecret
    slice-config:
      task-num: 8
      part-size: 104857600
	oss-config:
      qingyunOssClient1:
        bucket-name: Bucket1
      qingyunOssClient2:
        bucket-name: Bucket2
```

