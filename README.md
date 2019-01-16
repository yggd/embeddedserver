[![Apache license](http://img.shields.io/badge/license-APACHE2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# embeddedserver

Simple (S)FTP/TCP embedded server.

## How to Use

### Install.

To add this product to your project, the first thing to is to add in the API.

maven:

```xml
<dependency>
  <groupId>org.yggd.server</groupId>
  <artifactId>embeddedserver</artifactId>
  <version>0.1.0</version>
</dependency>
```

gradle:

```groovy
repositories {
  mavenCentral()
}

dependencies {
  compile group: 'org.yggd.server', name: 'embeddedserver', version: '0.1.0'
}
```

### FTP Server

1. Setup and start server

```java
final EmbeddedServer ftpServer = ServerBuilder.withFtp()
  .port(10021)
  .user(user -> {
    user.setName("user1");
    user.setPassword("password");
    user.setHomeDirectory("home/directory");
  })
  .filesystem(filesystem -> filesystem.setCreateHome(true))
  .build();

ftpServer.start();
```

For more information 'filesystem' above:
https://mina.apache.org/sshd-project/apidocs/org/apache/sshd/common/file/nativefs/NativeFileSystemFactory.html

2. Shutdown server

```java
ftpServer.stop();
```

### SFTP Server

"SFTP" means SSH File Transfer Protocol (not FTP over SSL/TLS).

1. generate SSH key

generate SSH public/private keyset.

example using OpenSSH below:

```bash
$ ssh-keygen -f ./id_rsa
  Generating public/private rsa key pair.
  Enter passphrase (empty for no passphrase):  <-- not displayed, remember your passphrase
  Enter same passphrase again: 
  Your identification has been saved in ./id_rsa.
  Your public key has been saved in ./id_rsa.pub.
  The key fingerprint is:
  SHA256:n8MD/mBF1xPk73i6VumcewpaYK82aAFkkkRo1Jok+Mo username@yourhost.
  The key's randomart image is:
  +---[RSA 2048]----+
  |...=o.       .o  |
  |o + + o      o . |
  | = o +    . . +  |
  |  +   .  . .   o |
  |..     .S +     o|
  |.E     ..* +   +.|
  |        +o* + +.+|
  |       .ooo* ..*.|
  |       . .+. .++o|
  +----[SHA256]-----+
$ ls
id_rsa		id_rsa.pub
$ 
```

2. Setup and start server

```java
final EmbeddedServer sftpServer = ServerBuilder.withSftp()
    .port(10022)
    .directory(Paths.get("home/directory"))
    .keyPairProvider(Paths.get("path-to-put/hostkey.ser")) // create ser file in the first running.
    .publicKeyAuthenticate(Paths.get("ssh-keys/id_rsa.pub"),
        (username, clientkey, session, serverkey) ->
            "user1".equals(username) && serverkey.equals(clientkey)) // implement client authentication.
    .build();

sftpServer.start();
```

Use "public" key in server.

3. Stop server

```java
sftpServer.stop();
```

You can also use [Spring Resources](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#resources) instead of Path.

```java
final EmbeddedServer sftpServer = ServerBuilder.withSftp()
    .port(PORT)
    .directory(home)
    .keyPairProvider(new ClassPathResource("/classpath/hostkey.ser"))
    .publicKeyAuthenticate(new ClassPathResource("/classpath/id_rsa.pub"),
        (username, clientkey, session, serverkey) ->
                USER.equals(username) && serverkey.equals(clientkey))
    .build();
```

You can test this embedded servers, using FTP Client: FileZilla, WinSCP and so on.
If you use those client in java code, refer test cases in this project.

## TCP Server

1. Setup and start server

```java
final EmbeddedServer tcpServer = ServerBuilder.withTcp()
    .port(PORT)
    .active( () ->  "response".getBytes())
    .read( b -> {
        try {
            serverRead.put(b);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    })
    .build();

tcpServer.start();
```

2. Stop server

```java
sftpServer.stop();
```
