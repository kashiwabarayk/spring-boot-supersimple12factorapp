# HTTPセッションをRedisに格納する
ここで関連する12Factorは以下の項目です。
* プロセス
* 廃棄容易性

ここではHTTPセッション情報を外部のRedisサーバに格納し、ステートレスなアプリケーションを実現します。

## ソースコードの編集
まずは依存関係を追加します。
`initial/pcfsample-initial/pom.xm`を以下の依存関係を追加します。
```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.session</groupId>
	<artifactId>spring-session</artifactId>
</dependency>
```
次に`src/main/java/com/example/pcfsample/PcfsampleappApplication.java`に以下のメソッドを追加します。
```java
@RequestMapping("/put")
String putSession(HttpSession session) {
	session.setAttribute("username", "<name> " + OffsetDateTime.now());
	return "Generated a session";
}

@RequestMapping("/get")
String getSession(HttpSession session) {
		return session.getAttribute("username").toString();
}

@RequestMapping("/remove")
	String removeSession(HttpSession session) {
		session.invalidate();
		return "Removed sessions";
	}
```
`<name>`に任意の名前を入力してください。

※`javax.servlet.http.HttpSession`をインポート分に追加してください。

また、以下のアノテーションを付与します。
```java
@SpringBootApplication
@RestController
@EnableCaching
@EnableRedisHttpSession
public class PcfsampleappApplication {
// . . . 
```
`@EnableRedisHttpSession`によってセッションがRedisに格納されます。

※`org.springframework.cache.annotation.Cacheable`, 
 `org.springframework.cache.annotation.EnableCaching`,
 `org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession`
をインポート分に追加してください。

次に以下のHttpSessionConfigクラスを作成します。

Spring SessionはRedisのconfigコマンドを使って初期化時にRedisの再設定を行いますが、PCFやAWSのRedisではconfigコマンドが無効化されていてエラーになってしまいます。

そのため、Spring SessionがRedisのCONFIGを実行しないようコンフィグレーションを作成します。
`/src/main/java/com/example/pcfsample/HttpSessionConfig.java`クラスを作成し、以下のメソッドを追加します。
```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession
@Configuration
public class HttpSessionConfig {
	@Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }
}

```

## アプリケーションのプッシュ
```bash
$ cd spring-boot-supersimple12factorapp/initial/pcfsample-initial
$ mvn clean package -DskipTests=true
$ cf push --no-start
```

## Redisインスタンスの作成とBind
先ほどと同様にRedisインスタンスを作成し、アプリケーションにBindします。
```bash
$ cf create-service p-redis shared-vm redis-session
$ cf bind-service myapp-<name> redis-session
$ cf env myapp-<name>
$ cf start myapp-<name>
```
Spring Bootにより、redis-sessionという名前のサービスがアプリケーションにBindされるとセッション情報がBindされているRedisに格納されるようになります。
こちらは参考情報なのでソースコードに追記する必要はございません。
```java
 // ..
   @Autowired
   @Qualifier("redis-session")
   RedisConnectionFactory rcf;
```

もしのこの機能をオフにしたい際は`application.properties`に以下の変更を加えてください。
(このハンズオンでは変更しないでください。)
```properties
spring.cloud.enabled=false
```

それでは動作を確認してみましょう。
```bash
$ curl -vvv http://myapp-<name>.<APP_DOMAIN>/put 2>&1 | grep Set-Cookie
< Set-Cookie: SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd ;path=/;HttpOnly

$ curl http://myapp-<name>.<APP_DOMAIN>/get -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
tkaburagi 2016-08-19T05:26:41.478Z%
```
セッション情報を取得できました。これがRedisに格納されていることを確認するためにアプリケーションを停止させます。
```bash
$ curl http://myapp-<name>.<APP_DOMAIN>/kill
502 Bad Gateway: Registered endpoint failed to handle the request.

$ curl http://myapp-<name>.<APP_DOMAIN>/get -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
404 Not Found: Requested route ('myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io') does not exist.
```
`System.exit(-1)`が実行されJVMが停止します。
PCFのコンテナリカバリ機能によってアプリケーションが自動再起動されますのでしばらくしたら以下のコマンドを実行します。
```bash
$ curl http://myapp-<name>.<APP_DOMAIN>/get -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
tkaburagi 2016-08-19T05:26:41.478Z%
```
再起動後もセッション情報が取得され、ローカルではなく外部にセッションが格納されていることがわかります。
セッションを削除して再度試したい時は以下のコマンドを実行してください。
```bash
$ curl http://myapp-<name>.<APP_DOMAIN>/remove -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
Removed sessions
```

## サービスをunbindした場合
サービスをunbindし、セッション情報がローカルのメモリに格納されているパターンの動作も確認してみましょう。
以下の行をコメントアウトします。
`PcfsampleappApplication`クラス
```java
//import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

//@EnableRedisHttpSession
```
`HttpSessionConfig`クラス
```java
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.session.data.redis.config.ConfigureRedisAction;
//import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

//@EnableRedisHttpSession
//@Configuration
public class HttpSessionConfig {
//	@Bean
//    public static ConfigureRedisAction configureRedisAction() {
//        return ConfigureRedisAction.NO_OP;
//    }
}
```
`pom.xml`ファイル
```xml
<!-- <dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.session</groupId>
	<artifactId>spring-session</artifactId>
</dependency> -->
```

```bash
$ mvn clean package -DskipTests=true
$ cf push
```
```bash
$ cf unbind-service myapp-<name> redis-session
$ cf restage myapp-<name>
```
Webブラウザから`put->get->kill->get`の順番でAPIを叩いてください。

![image](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/put.png)

![image](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/get1.png)

![image](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/kill.png)

![image](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/get2.png)


killでローカルのセッション情報が消えるため、2回目のgetでエラーが発生します。
※この演習が終わった後は「サービスをunbindした場合」の前の状態にアプリケーションを戻してから次に進んでください。

* `PcfsampleappApplication`クラスのコメントアウトの削除
* `HttpSessionConfig`クラスのコメントアウトの削除
* `pom.xml`ファイルのコメントアウトの削除
