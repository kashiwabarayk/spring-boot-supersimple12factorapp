# アプリケーションをスケールアウトする
ここで関連する12Factorは以下の項目です。
* 並行性

PCFを利用することで12Factorのアプリケーションの並行性を実現します。ここではアプリケーションをスケールアウトし、アプリケーションに対するルーティングなどもダイナミックに実現されることを確認します。

##アプリケーションの修正
スケールアウトの検証用に以下のメソッドを追加します。
```java
	@RequestMapping("/scaleout")
	String scaleout() throws JsonProcessingException, IOException {
		String vcap = System.getenv("VCAP_APPLICATION");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode vcap_app = mapper.readTree(vcap);
		return vcap_app.get("instance_index").asText();
	}
```
※ `com.fasterxml.jackson.core.JsonProcessingException`, `com.fasterxml.jackson.databind.JsonNode;`, `com.fasterxml.jackson.databind.ObjectMapper`インポート文に追加してください。

##アプリケーションのPush
```bash
$ mvn package -DskipTests=true
$ cf push
```

##アプリケーションのテスト
別ターミナル(ターミナル①)を起動し、以下のコマンドを実行します。
```console
$ while true; do cf app myapp-<name>; echo; sleep 1;done

Showing health and status for app myapp-tkaburagi-4 in org Handson / space user15 as user15...
OK

requested state: started
instances: 4/4
usage: 512M x 4 instances
urls: myapp-tkaburagi-4.cfapps.haas-67.pez.pivotal.io
last uploaded: Mon Sep 26 09:19:50 UTC 2016
stack: cflinuxfs2
buildpack: java-buildpack=v3.8.1-offline-https://github.com/cloudfoundry/java-buildpack.git#29c79f2 java-main open-jdk-like-jre=1.8.0_91-unlimited-crypto open-jdk-like-memory-calculator=2.0.2_RELEASE spring-auto-reconfiguration=1.10.0_RELEASE

     state     since                    cpu    memory           disk           details
#0   running   2016-09-26 06:20:42 PM   0.1%   360.3M of 512M   140.6M of 1G
```

別ターミナル(ターミナル②)を起動し、以下のコマンドを実行します。リクエストを受け付けているコンテナのインデックス番号が表示されます。
```console
$ while true; do curl http://myapp-<name>.<APP_DOMAIN>/scaleout; echo; sleep 1;done

0
0
0
0
```

##アプリケーションのスケールアウト
```bash
cf scale -i 4 myapp-<name>
```

ターミナル①を確認するとしばらくするとインスタンス数が4に増加していることがわかります。
```console
     state     since                    cpu    memory           disk           details
#0   running   2016-09-26 06:20:42 PM   0.2%   361.1M of 512M   140.6M of 1G
#1   running   2016-09-26 07:09:46 PM   0.4%   326.1M of 512M   140.6M of 1G
#2   running   2016-09-26 07:09:46 PM   0.3%   305.5M of 512M   140.6M of 1G
#3   running   2016-09-26 07:09:46 PM   0.4%   308.2M of 512M   140.6M of 1G
```

ターミナル②を確認するとスケールアウトしたインスタンスに自動的にリクエストが振られていることがわかります。
```concole
0
0
2
2
1
1
3
3
0
0
2
2
1
1
3
3
```
