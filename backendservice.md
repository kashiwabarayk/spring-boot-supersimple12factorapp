# バックエンドサービスを利用する
ここで関連する12Factorは以下の項目です。
* 依存関係
* 設定
* バックエンドサービス
* 開発/本番一致

ここではPCFのBindの機能とSpring Bootのspring-boot-starter-redisを利用してバックエンドのサービスをアタッチ(Bind)されたリソースとして扱います。これによってソースコードや設定ファイルに接続情報等をコーディングすることなく取得できます。
設定を外部から読み込むことで本番環境、開発環境やクラウドに対して可搬的なアプリケーションとなります。

## 準備
以下のコマンドで本プロジェクトをコピーします。
```bash
$ git clone https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp.git
```
```bash
$ cd initial/pcfsample-initial
```

## プロジェクトの編集
※IDEにMavenプロジェクトとしてインポートしてしまうと便利です。

pom.xmlに以下の依存関係を追加します。12 Factorでは依存関係はMavenやGradleなどを利用して明記しライブラリを取り込みます。
ここではRedisにデータをキャッシュするために必要なライブラリを指定します。
```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-redis</artifactId>
</dependency>
```

次に、src/main/java/com/example/pcfsample/PcfsampleappApplication.javaを以下のように編集します。
```java
package com.example.pcfsample;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableCaching
public class PcfsampleappApplication {

	@Autowired
	Greeter greeter;

	@RequestMapping("/")
	String home() {
		return greeter.hello();
	}

	@RequestMapping("/kill")
	String kill() {
		System.exit(-1);
		return "Killed";
	}

	public static void main(String[] args) {
		SpringApplication.run(PcfsampleappApplication.class, args);
	}

	@Component
	class Greeter {
		@Cacheable("hello")
		public String hello() {
			return "Hello. It's " + OffsetDateTime.now() + " now.";
		}
	}
}
```
`@EnableCaching`アノテーションによってキャッシュ機能を有効化します。また、`@Cacheable`によりメソッドの結果をキャッシュデータとして格納します。また該当データはキャッシュから取得されるようになります。

次に、PCFにデプロイするためのアプリケーションの定義情報を記入します。
initial/pcfsample-initial/manifest.ymlを以下のように編集します。
```yml
---
applications:
- name: myapp-<name> #自分の名前を記入
  host: myapp-<name> #自分の名前を記入
  memory: 512M
  instances: 1
  path: target/demo-initial-0.0.1-SNAPSHOT.jar
```

## アプリケーションのプッシュ
```bash
$ mvn clean package -DskipTests=true
```
まだRedisがサービスとしてアタッチされていないため`--no-start`オプションを付与し、アプリケーションをPCF上へアップロードします。
```bash
$ cf push --no-start
```

## Redisインスタンスの作成とBind
ここではデータのキャッシュ先として利用するRedisインスタンスの作成し、そのインスタンスをアプリケーションにアタッチ(Bind)します。
```bash
$ cf create-service p-redis shared-vm redis-caching
```
このコマンドにより自分用のRedisインスタンスがPCFによって払い出されます。

次に払い出されたインスタンスをアプリケーションにBindします。
```bash
$ cf bind-service myapp-<name> redis-caching
```

cf envコマンドを叩くことでBindされたRedisの情報を確認できます。
```console
$ cf env myapp-<name>

System-Provided:
{
 "VCAP_SERVICES": {
  "p-redis": [
   {
    "credentials": {
     "host": "10.65.202.115",
     "password": "f73b0ff4-6762-44d7-ac30-1af6f9ebef5b",
     "port": 33992
    },
    "label": "p-redis",
    "name": "redis-caching",
    "plan": "shared-vm",
    "provider": null,
    "syslog_drain_url": null,
    "tags": [
     "pivotal",
     "redis"
    ]
   }
  ]
 }
}

{
 "VCAP_APPLICATION": {
  "application_id": "66bcd390-37ff-4522-a14f-8e49ad608210",
  "application_name": "myapp-tkaburagi",
  "application_uris": [
   "myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io"
  ],
  "application_version": "d410fee2-b56e-41fa-9393-5ef7c5e561cd",
  "limits": {
   "disk": 1024,
   "fds": 16384,
   "mem": 512
  },
  "name": "myapp-tkaburagi,
  "space_id": "3bf40a1f-2afe-4fd1-8a7e-8b8c902ae1c3",
  "space_name": "development",
  "uris": [
   "myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io"
  ],
  "users": null,
  "version": "d410fee2-b56e-41fa-9393-5ef7c5e561cd"
 }
}

```
上記のようにBindされたRedisインスタンスの情報はアプリケーションの環境変数として取得できます。
Bindされたのでアプリケーションを起動します。
この環境変数はアプリケーション起動時に取得されます。
```bash
$ cf start myapp-<name>
```
## テスト
```bash
$ curl http://myapp-<name>.cfapps.haas-42.pez.pivotal.io

Hello. It's 2016-08-15T08:46:16.745Z now.
```

何回アクセスしてもキャッシュされているため、同じ結果が表示されることを確認してください。

ここでは、Redisに関する設定を全く行いませんでしたが、何が起きているのでしょうか。ログを見てみましょう。

``` console
$ cf logs myapp-<name> --recent
```

``` console
2016-03-17T22:19:50.04+0900 [APP/0]      OUT 2016-03-17 13:19:50.044  INFO 14 --- [           main] urceCloudServiceBeanFactoryPostProcessor : Auto-reconfiguring beans of type javax.sql.DataSource
2016-03-17T22:19:50.04+0900 [APP/0]      OUT 2016-03-17 13:19:50.047  INFO 14 --- [           main] urceCloudServiceBeanFactoryPostProcessor : No beans of type javax.sql.DataSource found. Skipping auto-reconfiguration.
2016-03-17T22:19:50.05+0900 [APP/0]      OUT 2016-03-17 13:19:50.051  INFO 14 --- [           main] edisCloudServiceBeanFactoryPostProcessor : Auto-reconfiguring beans of type org.springframework.data.redis.connection.RedisConnectionFactory
2016-03-17T22:19:50.11+0900 [APP/0]      OUT 2016-03-17 13:19:50.113  INFO 14 --- [           main] edisCloudServiceBeanFactoryPostProcessor : Reconfigured bean redisConnectionFactory into singleton service connector org.springframework.data.redis.connection.jedis.JedisConnectionFactory@51b6a3e3
```

`Reconfigured bean redisConnectionFactory`というメッセージが見えます。Java Buildpackに含まれるAuto Reconfigureという仕組みによって、サービスインスタンスのRedis情報から`RedisConnectionFactory` Beanを差し替えています。
これにより、アプリケーション側で特別な設定をすることなくCloud Foundry上でサービスインスタンスにアクセスすることができます。
ローカル開発時にローカル用Redisにアクセスする設定を行っている場合も、そのままCloud Foundryにデプロイして構いません。

Auto Reconfigureは**Springアプリケーション(とPlayアプリケーション)をデプロイした場合のみ有効**となる機能です。
[ドキュメント](https://github.com/cloudfoundry/java-buildpack-auto-reconfiguration#what-is-auto-reconfiguration)に記載されている通り、次のクラスのBeanが置換対象です。

* `javax.sql.DataSource`
* `org.springframework.amqp.rabbit.connection.ConnectionFactory`
* `org.springframework.data.mongodb.MongoDbFactory`
* `org.springframework.data.redis.connection.RedisConnectionFactory`
* `org.springframework.orm.hibernate3.AbstractSessionFactoryBean`
* `org.springframework.orm.hibernate4.LocalSessionFactoryBean`
* `org.springframework.orm.jpa.AbstractEntityManagerFactoryBean`

JSON内の`tags`の値やURLスキームを確認しています。

ただし、対象のBeanが複数定義ある場合(2つの`DataSource`など)や異なる同種サービスを同時に使う場合（MySQLとPostgreSQLなど）は、Beanの差し替えは発生しません。

Cloud Foundry (BuildPackの設定)でBeanのAuto Reconfigureされることにより接続先の情報が自動で設定され、今回のアプリケーションで使用しているSpring BootのAuto ConfigureによってRedisのキャッシュを使うための設定が自動化されています。

Spring Boot以外のアプリケーションを作成する場合は、環境変数`VCAP_SERVICES`に設定されているJSONをパースして接続先情報を取得します。`VCAP_SERVICES`の例を以下に示します。

``` json
{
  "rediscloud": [
   {
    "credentials": {
     "hostname": "pub-redis-13677.us-east-1-2.4.ec2.garantiadata.com",
     "password": "ChZds5T8YWxrK5Jx",
     "port": "13677"
    },
    "label": "rediscloud",
    "name": "myredis",
    "plan": "30mb",
    "provider": null,
    "syslog_drain_url": null,
    "tags": [
     "Data Stores",
     "Data Store",
     "Caching",
     "Messaging and Queuing",
     "key-value",
     "caching",
     "redis"
    ]
   }
  ]
 }
```


## Auto-configrationを利用しない方法
Auto Configuration機能を利用せずに明示的に環境変数から取得することも可能です。

まず、以下のコマンドでAuto configration機能をオフにします。
```bash
$ cf set-env myapp-<name> JBP_CONFIG_SPRING_AUTO_RECONFIGURATION '{enabled: false}'
$ cf set-env myapp-<name> SPRING_PROFILES_ACTIVE cloud # Auto 
$ cf restage myapp-<name>
```
Auto confgrationがオフになると設定ファイルから接続情報が取得されます。

現在設定ファイルはからの状態なのでデフォルトでは`localhost`のRedisサーバにアクセスを試行します。
そのため、設定ファイルなしだとアプリケーションの起動に失敗します。
設定ファイルから取得した値が利用されるようになったことがわかります。

環境変数を読み込むための設定を`application.properties`ファイルに記載します。
これによりどの環境でもソースコードの変更なくアプリケーションが稼働します。
initial/pcfsample-initial/src/main/resources/application.properties を以下のように編集します。
```properties
spring.redis.host=${vcap.services.redis-caching.credentials.host}
spring.redis.port=${vcap.services.redis-caching.credentials.port}
spring.redis.password=${vcap.services.redis-caching.credentials.password}
```
```bash
$ mvn clean package -DskipTests=true
```
```bash
$ cf push
```
設定ファイルのRedisに接続し、アプリケーションが正常に起動しました。
同じようにAPIにアクセスして動作を確認してみましょう。
