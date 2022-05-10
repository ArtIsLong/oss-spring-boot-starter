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
    <groupId>com.netease.cloud</groupId>
    <artifactId>nos-sdk-java-publiccloud</artifactId>
    <version>1.3.1</version>
</dependency>
```

通过`oss.wangyi.enable=true`开启网易数帆OSS对象存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个网易数帆OSS对象存储配置

```yaml
oss:
  wangyi:
    enable: true
    endpoint: Endpoint
    access-key: AccessKey
    secret-key: SecretKey
    bucket-name: Bucket
    base-path: /
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启网易数帆OSS对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(WangYiOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个网易数帆OSS对象存储配置

```yaml
oss:
  wangyi:
    enable: true
    oss-config:
      wangyiOssClient1:
        endpoint: Endpoint
        access-key: AccessKey
        secret-key: SecretKey
        bucket-name: Bucket1
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
        client-config:
          region: cos.ap-chengdu
      wangyiOssClient2:
        endpoint: Endpoint
        access-key: AccessKey
        secret-key: SecretKey
        bucket-name: Bucket2
        base-path: /  # 存储根路径，默认路径为 /
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个网易数帆OSS对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("wangyiOssClient1")
private StandardOssClient wangyiOssClient1;
@Autowired
@Qualifier("wangyiOssClient2")
private StandardOssClient wangyiOssClient2;
```

- 客户端自定义配置

可通过oss.wangyi.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.wangyi.model.WangYiOssClientConfig`类查看。

![image-20220510005303002](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220510005303002.png)

示例如下：

```yaml
oss:
  wangyi:
    enable: true
    endpoint: Endpoint
    access-key: AccessKey
    secret-key: SecretKey
    bucket-name: Bucket
    base-path: /  # 存储根路径，默认路径为 /
    client-config:
      connection-timeout: 50000
      max-connections: 50
      # ...... 
```

- 配置复用

当使用同一个网易数帆OSS对象存储多个不同Bucket时，可复用endpoint、secretKey、accessKey、clientConfig，只需要配置Bucket相关参数即可，示例如下：

```yaml
oss:
  wangyi:
    enable: true
    endpoint: Endpoint
    access-key: AccessKey
    secret-key: SecretKey
    client-config:
      connection-timeout: 50000
      max-connections: 50
      # ...... 
    oss-config:
      wangyiOssClient1:
        bucket-name: Bucket1
      wangyiOssClient2:
        bucket-name: Bucket2
```

