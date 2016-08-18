# HTTPセッションをRedisに格納する
ここで関連する12Factorは以下の項目です。
* プロセス
* 廃棄容易性

## ソースコードの編集
```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
```java
@RequestMapping("/put")
String putSession(HttpSession session) {
	session.setAttribute("username", "tkaburagi");
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

## アプリケーションのプッシュ

```bash
$ ./mvnw clean package
$ cf push --nostart
```

## Redisインスタンスの作成とBind
```bash
$ cf create-service p-redis redis-session
$ cf bind-service myapp-<name> redis-session
$ cf env myapp-<name>
$ cf start myapp-<name>
```

## サービスをunbindした場合
