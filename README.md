# spring-boot-supersimple12factorapp
Spring BootとPivotal Cloud Foundryの機能を使ってとてもシンプルな12Factor Applicationを実装するハンズオンです。
12 Factorに関しては[こちら](https://12factor.net/ja/)をご参照ください。
この中の以下の項目について扱います。
* 依存関係
* 設定
* バックエンドサービス
* プロセス
* ポートバインディング
* 廃棄容易性
* ログ
* 開発/本番一致

#事前準備
以下の準備をして下さい。
* Pivotal Cloud FoundryかPCF Devのインストール、もしくはPivotal Web Servicesのアカウント作成
* mvn cliのインストール
* Java 8のインストール
* git cliのインストール
* cf cliのインストール

# ハンズオン手順
1. [シンプルなアプリケーションをデプロイする](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/deploy.md)
2. [バックエンドサービスを利用する](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/backendservice.md)
3. [HTTPセッションを外部に格納する](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/stateless.md)
4. [ログをストリーミングイベントとして扱う](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/logstreaming.md)
5. [Blue Greenデプロイする](https://github.com/tkaburagi1214/spring-boot-supersimple12factorapp/blob/master/bgdeploy.md)
