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
// . . . 
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
$ cf log myapp-<name>
```
別の端末を立ち上げ、以下のコマンドを実行します。
```bash
$ curl http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/logger
Output logs%
```
最初の端末に以下のログが出力されます。
```console
016-08-19T15:26:22.42+0900 [APP/0]      OUT 2016-08-19 06:26:22.424  INFO 21 --- [nio-8080-exec-9] c.e.pcfsample.PcfsampleappApplication    : Called Logger
2016-08-19T15:26:22.42+0900 [APP/0]      OUT 2016-08-19 06:26:22.425 ERROR 21 --- [nio-8080-exec-9] c.e.pcfsample.PcfsampleappApplication    : Error Logger!!
```

なぜPCFのAPI経由でアプリケーションコンテナの標準出力にストリーミングされているログが見れるのでしょうか？
＜後で＞

## Papertrailの利用
PCFから収集されたログはAPI経由だけでなく、Syslogで外部に転送できます。Syslogに対応しているソフトウェアなら何でもできますがここでは無料でアカウントを作成できるPapertrailを利用します。
Papertrailのアカウント後に取得できる<HOST:IP>をメモしてください。
```bash
$ cf create-user-provided-service logdrainer -r <syslog-tls://<HOST:IP>>
$ cf bind-service myapp-<name> logdrainer
$ cf restage myapp-<name>
$ cf env myapp-<name>
```
以下のように環境変数に Syslogのエンドポイントがセットされます。
PCFのDopplerサーバよりSyslogはここに転送されます。
```json
System-Provided:
{
 "VCAP_SERVICES": {
  "user-provided": [
   {
    "credentials": {},
    "label": "user-provided",
    "name": "logdrainer",
    "syslog_drain_url": "syslog-tls://logs4.papertrailapp.com:12412",
    "tags": []
   }
  ]
 }
}
```

次に、アプリケーションへHTTPリクエストし、Papertrailの出力結果を見ています。
```bash
$ curl http://myapp-tkaburagi.cfapps.haas-42.pez.pivotal.io/logger
Output logs%
```

Papertrailのダッシュボードにログが表示されていることを確認します。
![image](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/Screen%20Shot%202016-08-19%20at%204.49.40%20PM.png)
