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
	session.setAttribute("username", "<name>");
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
<name>に任意の名前を入力してください。

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
PCFのBuildpackの機能により、redis-sessionという名前のサービスがアプリケーションにBindされるとセッション情報がBindされているRedisに格納されるようになります。

それでは動作を確認してみましょう。
＜後で追加＞

## サービスをunbindした場合
サービスをunbindし、セッション情報がローカルのメモリに格納されているパターンの動作も確認してみましょう。
＜後で追加＞
