## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
```

通过`oss.local.enable=true`开启本地存储自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个本地存储配置

```yaml
oss:
  local:
    enable: true
    base-path: 本地对象存储路径
    slice-config:
      task-num: 8
      part-size: 104857600
```

通过如上配置即可开启本地存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(LocalOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个本地存储配置

```yaml
oss:
  local:
    enable: true
    oss-config:
      localOssClient1:
	    base-path: F:/data/
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
      localOssClient2:
 	    base-path: E:/data/
        slice-config:
          task-num: 8  # 并发线程数,默认等于CPU的核数
          part-size: 104857600  # 分片大小,单位KB,默认5MB
```

当配置多个本地对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("localOssClient1")
private StandardOssClient localOssClient1;
@Autowired
@Qualifier("localOssClient2")
private StandardOssClient localOssClient2;
```

