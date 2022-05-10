## 开始使用

引入以下依赖

```xml
<dependencies>
	<groupId>io.github.artislong</groupId>
    <artifactId>oss-spring-boot-starter</artifactId>
    <version>{latest.version}</version>
</dependencies>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<!-- 数据库驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
<dependency>
    <groupId>com.oracle</groupId>
    <artifactId>ojdbc6</artifactId>
    <version>11.2.0.3</version>
</dependency>
```

通过`oss.jdbc.enable=true`开启Jdbc对象存储自动配置，目前只支持Oracle、MySQL对象存储，建表脚本见jdbc目录。

## 配置详解

当系统本身不需要数据源时，记得使用`@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)`注解排除数据源自动配置。

- 单个Jdbc对象存储配置

```yaml
oss:
  jdbc:
    enable: true
    base-path: /
    type: com.zaxxer.hikari.HikariDataSource
    url: jdbc:mysql://127.0.0.1:3306/oss?characterEncoding=UTF-8&useUnicode=true&useSSL=false&tinyInt1isBit=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    driver: com.mysql.cj.jdbc.Driver
    username: root
    password: root
```

通过如上配置即可开启Jdbc对象存储，可通过以下方式注入标准的OSS客户端。

```java
@Autowired
@Qualifier(JdbcOssConfiguration.DEFAULT_BEAN_NAME)
private StandardOssClient ossClient;
```

- 多个Jdbc对象存储配置

```yaml
oss:
  jdbc:
    enable: true
    oss-config:
      mysqlOssClient:
        base-path: /
        type: com.zaxxer.hikari.HikariDataSource
        url: jdbc:mysql://127.0.0.1:3306/oss?characterEncoding=UTF-8&useUnicode=true&useSSL=false&tinyInt1isBit=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
        driver: com.mysql.cj.jdbc.Driver
        username: root
        password: root
      oracleOssClient:
        base-path: /
        type: com.zaxxer.hikari.HikariDataSource
        url: jdbc:oracle:thin:@127.0.0.1:1521:orcl
        driver: oracle.jdbc.driver.OracleDriver
        username: oss
        password: oss123
```

当配置多个Jdbc对象存储时，使用如下方式注入

```java
@Autowired
@Qualifier("mysqlOssClient")
private StandardOssClient mysqlOssClient;
@Autowired
@Qualifier("oracleOssClient")
private StandardOssClient oracleOssClient;
```

- 使用系统默认数据源

  当系统本身已经配置了数据源，此时不需要单独配置对象存储数据源，对象存储将采用系统默认数据源

```yaml
oss:
  jdbc:
    enable: true
    base-path: /
```

- 系统存在多个数据源Bean

  注意：是多个数据源Bean对象，不是通过`AbstractDataSource`实现的多数据源配置

```yaml
oss:
  jdbc:
    enable: true
    base-path: /
    data-source-name: 数据源Bean名称
```

- 系统存在多个数据源Bean，配置多个Jdbc对象存储时

  在系统配置了多个数据源Bean对象，且需要在多个数据源中使用对象存储，可通过**多个Jdbc对象存储配置**时配置数据源Bean名称来实现

```yaml
oss:
  jdbc:
    enable: true
    oss-config:
      mysqlOssClient:
        base-path: /
        data-source-name: 数据源Bean名称
      oracleOssClient:
        base-path: /
        data-source-name: 数据源Bean名称
```

