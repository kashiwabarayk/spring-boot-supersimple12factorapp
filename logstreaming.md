# ログをストリーミングイベントとして扱う
ここで関連する12Factorは以下の項目です。
* ログ
* 廃棄容易性

ここでは12Factorでのロギング方法を扱います。12Factorではログはファイル出力ではなく、ストリーミングイベントとして扱うことが推奨されています。これによりコンテナ対応やアプリケーションの廃棄容易性を確保することができます。

## ソースコードの編集
Spring Bootのロガーの機能はすでに追加してあるspring-boot-starter-web内にあるためここではpom.xmlの編集は行いません。Javaのファイルを下記のように編集します。
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
Spring Bootではデフォルトで標準出力にログを出力します。

## アプリケーションのプッシュ
```bash
$ ./mvnw clean package
$ cf push
```

## ログの確認
```bash
cf log myapp-<name> --recent
```
なぜPCFのAPI経由でアプリケーションコンテナの標準出力にストリーミングされているログが見れるのでしょうか？
＜後で＞

## Papertrailの利用
PCFから収集されたログはAPI経由だけでなく、Syslogで外部に転送できます。Syslogに対応しているソフトウェアなら何でもできますがここでは無料でアカウントを作成できるPapertrailを利用します。
Papertrailのアカウント後に取得できる<HOST:IP>をメモしてください。
```bash
$ cf create-user-provided-service logdrainer -r <syslog://<HOST:IP>>
$ cf bind-service myapp-<name> logdrainer
$ cf restart myapp-<name>
```

次に、アプリケーションへHTTPリクエストし、Papertrailの出力結果を見ています。
＜追記予定＞
