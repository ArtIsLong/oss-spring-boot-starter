## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>commons-net</groupId>
    <artifactId>commons-net</artifactId>
    <version>3.8.0</version>
</dependency>
```

通过`oss.ftp.enable=true`开启FTP自动配置

## 配置详解

**注：**所有具有默认值的属性，配置都可缺省，且不支持复用。

- 单个FTP配置

```yaml
oss:
  ftp:
    enable: true
    host: FTP服务器IP
    port: FTP服务端口
    user: 用户名
    password: 密码
    charset: 编码
    base-path: FTP服务器存储根路径
```

通过如上配置即可开启FTP，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(FtpOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个FTP配置

```yaml
oss:
  ftp:
    enable: true
    oss-config:
      ftpOssClient1:
        host: FTP服务器IP
        port: FTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: FTP服务器存储根路径
      ftpOssClient2:
        host: FTP服务器IP
        port: FTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: FTP服务器存储根路径
```

当配置多个FTP时，使用如下方式注入

```java
@Autowired
@Qualifier("ftpOssClient1")
private StandardOssClient ftpOssClient1;
@Autowired
@Qualifier("ftpOssClient2")
private StandardOssClient ftpOssClient2;
```

- 客户端自定义配置

可通过oss.ftp.client-config.XXX来配置，XXX具体值可通过`io.github.artislong.core.ftp.model.FtpOssClientConfig`类查看。

![image-20220428153611802](C:\Users\15221\AppData\Roaming\Typora\typora-user-images\image-20220428153611802.png)

示例如下：

```yaml
oss:
  ftp:
    enable: true
    host: FTP服务器IP
    port: FTP服务端口
    user: 用户名
    password: 密码
    charset: 编码
    base-path: FTP服务器存储根路径
    client-config:
      mode: Passive
      back-to-pwd: false
      # ...... 
```

- 配置复用

由于FTP服务不同于标准的OSS对象存储，所以基本配置不支持复用，仅对client-config属性进行复用配置支持。

```yaml
oss:
  ftp:
    enable: true
    client-config:
      mode: Passive
      back-to-pwd: false
      # ...... 
    oss-config:
      ftpOssClient1:
        host: FTP服务器IP
        port: FTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: FTP服务器存储根路径
      ftpOssClient2:
        host: FTP服务器IP
        port: FTP服务端口
        user: 用户名
        password: 密码
        charset: 编码
        base-path: FTP服务器存储根路径
```

