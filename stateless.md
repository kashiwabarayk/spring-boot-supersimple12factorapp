# HTTPセッションをRedisに格納する
ここで関連する12Factorは以下の項目です。
* プロセス
* 廃棄容易性

ここではHTTPセッション情報を外部のRedisサーバに格納し、ステートレスなアプリケーションを実現します。

## ソースコードの編集
まずは依存関係を追加します。
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
次に以下のメソッドを追加します。
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

## アプリケーションのプッシュ
```bash
$ ./mvnw clean package
$ cf push --nostart
```

## Redisインスタンスの作成とBind
先ほどと同様にRedisインスタンスを作成し、アプリケーションにBindします。
```bash
$ cf create-service p-redis redis-session
$ cf bind-service myapp-<name> redis-session
$ cf env myapp-<name>
$ cf start myapp-<name>
```
Spring Bootにより、redis-sessionという名前のサービスがアプリケーションにBindされるとセッション情報がBindされているRedisに格納されるようになります。
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
$ curl -vvv http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/put
curl -vvv -X PUT http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/put
*   Trying 209.194.245.136...
* Connected to myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io (209.194.245.136) port 80 (#0)
> PUT /put HTTP/1.1
> Host: myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io
> User-Agent: curl/7.43.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Length: 19
< Content-Type: text/plain;charset=UTF-8
< Date: Fri, 19 Aug 2016 05:15:12 GMT
< Set-Cookie: SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd;path=/;HttpOnly
< X-Vcap-Request-Id: acec9403-a7c2-4d7e-65a3-09d7d2704550
<
* Connection #0 to host myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io left intact
Generated a session%

$ curl http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/get -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
tkaburagi 2016-08-19T05:26:41.478Z%
```
セッション情報を取得できました。これがRedisに格納されていることを確認するためにアプリケーションを停止させます。
```bash
$ curl http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/kill
$ curl http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/get -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
404 Not Found: Requested route ('myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io') does not exist.
```
`System.exit(-1)`が実行されJVMが停止します。
PCFのコンテナリカバリ機能によってアプリケーションが自動再起動されますのでしばらくしたら以下のコマンドを実行します。
```bash
$ curl http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/get -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
tkaburagi 2016-08-19T05:26:41.478Z%
```
再起動後もセッション情報が取得され、ローカルではなく外部にセッションが格納されていることがわかります。
セッションを削除して再度試したい時は以下のコマンドを実行してください。
```bash
$ curl http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/remove -b SESSION=c83680ac-45a7-450c-86df-876f8fcb9fcd
Removed sessions
```

## サービスをunbindした場合
サービスをunbindし、セッション情報がローカルのメモリに格納されているパターンの動作も確認してみましょう。
```java
//@EnableRedisHttpSession
```
```bash
$ cf./mvnw clean package -DskipTests=true
$ cf push
```
```bash
$ cf unbind-service myapp-tkaburagi redis-session
$ cf restage myapp-tkaburagi
```
