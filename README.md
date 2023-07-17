# port-forwarder
まとめて複数のものをsshでポートフォワードするためのネイティブツール。
GraalVM対応で、Javaの実行環境がなくても動作する。

# Usage
## option
```
--config="config.yml"
    YAMLファイルを指定する

--port="8080"
    WebGUIのポートを指定する。
    ほぼ未実装。
    指定が無ければ起動しない。

--debug
    詳細なスタックトレースを出力する
```

## config
`destination`と`key`の指定には`command`か`text`が使用できる。
`forward`はkeyにローカルの情報、valueにリモートホストとポートを指定する。
```
destination:
  command: |
    echo "admin@example.com"

key:
  text: ~/.ssh/key

forward:
  localhost:10002: example1.internal:8080
  172.17.0.1:10003: example2.internal:8080
```
