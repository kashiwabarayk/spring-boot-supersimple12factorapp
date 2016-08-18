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

```bash
$ ./mvnw clean package
$ cf push
```
