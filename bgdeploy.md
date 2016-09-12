# Blue Greenデプロイする
ここで関連する12Factorは以下の項目です。
* 廃棄容易性

ここではアプリケーションの無停止更新を実施します。12Factorでステートレスなアプリケーションを実装することで最小限の運用で無停止更新を実現できます。

## ソースコードの編集
以下のメソッドを追加します。
```java
@RequestMapping("/bg")
String bgdemo(HttpSession session) {
	String sep = System.getProperty("line.separator");
	return "Version: 1." + sep
		+ "Java Version: " + System.getProperty("java.version") + sep
		+ "Session: " + session.getAttribute("username").toString();
}
```

## アプリケーションのプッシュ
```bash
$ mvn clean package -DskipTests=true
$ cf push
```

##テスト
```bash 
$ curl -vvv http://myapp-<name>.cfapps.haas-42.pez.pivotal.io/put 2>&1 | grep Set-Cookie
< Set-Cookie: *SESSION=bb1b3a64-90c2-4760-b268-9225bbcdc623*;path=/;HttpOnly
```
```bash
$ curl http://myapp-<name>.cfapps.haas-42.pez.pivotal.io/bgdemo -b SESSION=bb1b3a64-90c2-4760-b268-9225bbcdc623
Version: 1.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:11:46.857Z%
```

## アプリケーションの更新
別の端末を立ち上げ、以下のコマンドを実行しアプリケーションに毎秒アクセスします。
```bash
$ while true; do curl http://myapp-<name>.cfapps.haas-42.pez.pivotal.io/bg -b SESSION=bb1b3a64-90c2-4760-b268-9225bbcdc623; echo; sleep 1;done
```

##アプリケーションのバージョンアップ
先ほどの`bgdemo`メソッドを以下のように編集します。
```java
@RequestMapping("/bg")
String bgdemo(HttpSession session) {
	String sep = System.getProperty("line.separator");
	return "Version: 2." + sep
		+ "Java Version: " + System.getProperty("java.version") + sep
		+ "Session: " + session.getAttribute("username").toString();
}
```
```bash
$ mvn clean package -DskipTests=true
```
`manifest.yml`を以下のように編集します。
```yml
---
applications:
- name: myapp-<name>-v2
  host: myapp-<name>-temp
  memory: 512M
  instances: 1
  path: target/demo-initial-0.0.1-SNAPSHOT.jar
  services:
   - redis-session
   - redis-caching
```
```bash
$ cf push
```

## 新しいアプリケーションの確認
起動をしたら以下のコマンドで動作を確認します。
```bash
$ curl http://myapp-<name>-temp.cfapps.haas-42.pez.pivotal.io/bg -b SESSION=bb1b3a64-90c2-4760-b268-9225bbcdc623
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:11:46.857Z%
```
`-temp`とあるようにこの状態ではまだ本番のURLからはアクセスを受け入れていません。

それでは動作に問題ないことを確認してから本番のURLにマッピングを行います。
別ターミナルで以下のコマンドを実行してください。
```bash
cf map-route myapp-<name>-v2 cfapps.haas-42.pez.pivotal.io --hostname myapp-<name>
```
curlで毎秒アクセスしている端末の出力結果にVerion 2のアプリケーションの結果が出力されています。
```console
Version: 1.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 1.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 1.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
```
次にVersion 1のアプリケーションを本番のURLから取り除きます。
```bash
cf unmap-route myapp-<name> cfapps.haas-42.pez.pivotal.io --hostname myapp-<name>
```
curlで毎秒アクセスしている端末の出力結果を確認します。
```console
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
Version: 2.
Java Version: 1.8.0_91
Session: tkaburagi 2016-08-19T08:57:16.411Z
```
Version 1のアプリケーションが負荷分散先から切り離され、ユーザからは見えない状態となります。
あとは`-temp`をunmapし、旧バージョンのアプリケーションを削除するだけです。
```bash
$ cf unmap-route myapp-<name>-v2 cfapps.haas-42.pez.pivotal.io --hostname myapp-<name>-temp
$ cf delete myapp-<name>

Really delete the app myapp-tkaburagi?> y
```

今回はアプリケーションのアップグレードでしたが、Javaのアップグレードなども全く同じオペレーションで実現できます。
