## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
```

通过`oss.sftp.enable=true`开启FTP自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个SFTP配置

```yaml
oss:
  sftp:
    enable: true
    host: SFTP服务器IP
    port: SFTP服务端口
    user: 用户名
    password: 密码
    charset: 编码
    base-path: FTP服务器存储根路径
```

通过如上配置即可开启SFTP，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(SftpOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个FTP配置

```yaml
oss:
  sftp:
    enable: true
    oss-config:
      sftpOssClient1:
        host: SFTP服务器IP
        port: SFTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: SFTP服务器存储根路径
      sftpOssClient2:
        host: SFTP服务器IP
        port: SFTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: SFTP服务器存储根路径
```

当配置多个SFTP时，使用如下方式注入

```java
@Autowired
@Qualifier("sftpOssClient1")
private StandardOssClient sftpOssClient1;
@Autowired
@Qualifier("sftpOssClient2")
private StandardOssClient sftpOssClient2;
```

- 客户端自定义配置

可通过oss.sftp.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.sftp.model.SftpOssClientConfig`类查看。

![image-20220509003606597](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220509003606597.png)

示例如下：

```yaml
oss:
  sftp:
    enable: true
    host: SFTP服务器IP
    port: SFTP服务端口
    user: 用户名
    password: 密码
    charset: 编码
    base-path: SFTP服务器存储根路径
    client-config:
      connection-timeout: 10000
      so-timeout: 10000
      # ...... 
```

- 配置复用

由于SFTP服务不同于标准的OSS对象存储，所以基本配置不支持复用，仅对client-config属性进行复用配置支持。

```yaml
oss:
  sftp:
    enable: true
    client-config:
      connection-timeout: 10000
      so-timeout: 10000
      # ...... 
    oss-config:
      sftpOssClient1:
        host: FTP服务器IP
        port: SFTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: SFTP服务器存储根路径
      sftpOssClient2:
        host: SFTP服务器IP
        port: SFTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: SFTP服务器存储根路径
```

