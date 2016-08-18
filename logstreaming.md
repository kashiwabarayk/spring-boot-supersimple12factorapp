# ログをストリーミングイベントとして扱う
ここで関連する12Factorは以下の項目です。
* ログ
* 廃棄容易性

## ソースコードの編集
```java
public class PcfsampleappApplication {
	private static final Logger logger = LoggerFactory.getLogger(PcfsampleappApplication.class);
	
	~~~~~
	
```

```java
@RequestMapping("/logger")
	String logger() {
		logger.info("Called Logger");
		logger.error("Error Logger!!");
		return "Output logs";
	}
```

## アプリケーションのプッシュ
```bash
$ ./mvnw clean package
$ cf push
```

## ログの確認
```text
```

## Papertrailの利用
```bash
$ cf create-user-provided-service logdrainer -r <URI>
$ cf restart myapp-<name>
```
