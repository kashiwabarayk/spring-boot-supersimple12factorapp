# バックエンドサービスを利用する
ここで関連する12Factorは以下の項目です。
* 依存関係
* 設定
* バックエンドサービス
* 開発/本番一致

ここではPCFのBindの機能とSpring Bootのspring-boot-starter-redisを利用してバックエンドのサービスをアタッチ(Bind)されたリソースとして扱います。これによってソースコードや設定ファイルに接続情報等をコーディングすることなく取得できます。設定を外部から読み込むことで本番環境、開発環境やマルチクラウドに対して可搬的なアプリケーションとなります。

## 準備
以下のコマンドで本プロジェクトをコピーします。
```bash
$ git clone https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp.git
```
```bash
$ cd initial/pcfsample-initial
```

## プロジェクトの編集
pom.xmlに以下の依存関係を追加します。12 Factorでは依存関係はMavenやGradleなどを利用して明記しライブラリを取り込みます。ここではRedisにデータをキャッシュするために必要なライブラリを指定します。
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

次に、PcfsampleappApplication.javaを以下のように編集します。
```java
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

@Component
class Greeter {
	@Cacheable("hello")
	public String hello() {
		return "Hello. It's " + OffsetDateTime.now() + " now.";
	}
}
```
@EnableCachingアノテーションによってキャッシュ機能を有効化します。また@Cacheableによりメソッドの結果をキャッシュデータとして格納します。また該当データはキャッシュから取得されるようになります。

```yml
---
applications:
- name: myapp-<name> #自分の名前を記入
  memory: 512M
  instances: 1
  path: target/demo-0.0.1-SNAPSHOT.jar
```

## アプリケーションのプッシュ
```bash
$ ./mvnw clean package -DskipTests=true
```
まだRedisがサービスとしてアタッチされていないため--no-startオプションを付与し、アプリケーションをPCF上へアップロードします。
```bash
$ cf push --no-start
```

## Redisインスタンスの作成とBind
ここではデータのキャッシュ先として利用するRedisインスタンスの作成し、そのインスタンスをアプリケーションにアタッチ(Bind)します。
```bash
$ cf create-service p-redis redis-caching
```
このコマンドにより自分用のRedisインスタンスがPCFによって払い出されます。
次に払い出されたインスタンスをアプリケーションにBindします。
```bash
$ cf bind-service myapp-<name> redis-caching
```

cf envコマンドを叩くことでBindされたRedisの情報を確認できます。
```bash
$ cf env myapp-<name>
```
上記のようにBindされたRedisインスタンスの情報はアプリケーションの環境変数として取得できます。Bindされたのでアプリケーションを起動します。この環境変数はアプリケーション起動時に取得されます。
```bash
$ cf start myapp-<name>
```
＜追記予定＞

## Auto-configrationを利用しない方法
Auto Configuration機能を利用せずに明示的に環境変数から取得することも可能です。まず、以下のコマンドでAuto configration機能をオフにします。
```bash
$ cf set-env myapp-<name> JBP_CONFIG_SPRING_AUTO_RECONFIGURATION '{enabled: false}'
$ cf set-env myapp-<name> SPRING_PROFILES_ACTIVE cloud # Auto 
```

環境変数を読み込むための設定をapplication.propertiesファイルに記載します。これによりどの環境でもソースコードの変更なくアプリケーションが稼働します。
```properties
spring.redis.host=${vcap.services.redis-caching.credentials.host}
spring.redis.port=${vcap.services.redis-caching.credentials.port}
spring.redis.password=${vcap.services.redis-caching.credentials.password}
```
```bash
$ ./mvnw clean package
```
```bash
$ cf push
```

同じようにAPIにアクセスしてみましょう。

## 念のため確認
JBP_CONFIG_SPRING_AUTO_RECONFIGURATION '{enabled: false}' の設定をした状態でapplication.propertiesの接続情報を削除し、ビルド→プッシュしてみましょう。
```properties
#spring.redis.host=${vcap.services.redis-caching.credentials.host}
#spring.redis.port=${vcap.services.redis-caching.credentials.port}
#spring.redis.password=${vcap.services.redis-caching.credentials.password}
```
```bash
$ ./mvnw clean package -DskipTests=true
```
```bash
$ cf push
```
デフォルトではlocalhostのRedisサーバにアクセスを試行するため、設定ファイルなしだとアプリケーションの起動に失敗します。
