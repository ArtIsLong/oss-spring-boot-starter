<font color="red" style='font-size:18'>暂无测试环境，待测试</font>

## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>cn.ucloud.ufile</groupId>
    <artifactId>ufile-client-java</artifactId>
    <version>2.6.6</version>
</dependency>
```

通过`oss.ucloud.enable=true`开启Ucloud OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个Ucloud OSS对象存储配置

```yaml
oss:
  ucloud:
    enable: true
    public-key: PublicKey
    private-key: PrivateKey
    custom-host: CustomHost
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启Ucloud OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(UcloudOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个Ucloud OSS对象存储配置

```yaml
oss:
  ucloud:
    enable: true
    oss-config:
      ucloudOssClient1:
        public-key: PublicKey
	    private-key: PrivateKey
    	custom-host: CustomHost
        bucket-name: Bucket1
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      ucloudOssClient2:
        public-key: PublicKey
	    private-key: PrivateKey
    	custom-host: CustomHost
        bucket-name: Bucket2
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个Ucloud OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("ucloudOssClient1")
private StandardOssClient ucloudOssClient1;
@Autowired
@Qualifier("ucloudOssClient2")
private StandardOssClient ucloudOssClient2;
```

- 客户端自定义配置

可通过oss.ucloud.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.ucloud.model.UcloudOssClientConfig`类查看。

![image-20220510003225426](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220510003225426.png)

示例如下：

```yaml
oss:
  ucloud:
    enable: true
    public-key: PublicKey
    private-key: PrivateKey
    custom-host: CustomHost
    bucket-name: Bucket
    base-path: /  # 存储根路径，默认路径为 /
    client-config:
      timeout-connect: 50000
      timeout-read: 50000
      # ...... 
```

- 配置复用

当使用同一个Ucloud OSS对象存储多个不同Bucket时，可复用publicKey、privateKey、customHost、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  ucloud:
    enable: true
    public-key: PublicKey
    private-key: PrivateKey
    custom-host: CustomHost
    client-config:
      timeout-connect: 50000
      timeout-read: 50000
      # ...... 
    oss-config:
      ucloudOssClient1:
        bucket-name: Bucket1
      ucloudOssClient2:
        bucket-name: Bucket2
```

