# バックエンドサービスを利用する
ここで関連する12Factorは以下の項目です。
* 依存関係
* 設定
* バックエンドサービス
* 開発/本番一致

## 準備
以下のコマンドで本プロジェクトをコピーします。
```bash
$ git clone https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp.git
```
```bash
$ cd initial/pcfsample-initial
```

## プロジェクトの編集
pom.xmlに以下の依存関係を追加します。
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

PcfsampleappApplication.javaを以下のように編集します。
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

```yml
---
applications:
- name: myapp-<name> #自分の名前を記入
  memory: 512M
  instances: 1
  path: target/demo-0.0.1-SNAPSHOT.jar
```

```bash
$ ./mvnw clean package
$ cf push --no-start
```

## Redisサービスの作成とBind
```bash
$ cf create-service p-redis redis-caching
```
```bash
$ cf bind-service myapp-<name> redis caching
```
```bash
$ cf env myapp-<name>
```
```bash
$ cf start myapp-<name>
```

## Auto-configrationを利用しない方法
```properties
spring.redis.host=${vcap.services.redis-caching.credentials.host}
spring.redis.port=${vcap.services.redis-caching.credentials.port}
spring.redis.password=${vcap.services.redis-caching.credentials.password}
```

```bash
```
