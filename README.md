# port-forwarder
まとめて複数のものをsshでポートフォワーめのネイティブツール。
GraalVM対応で、Javaの実行環境がなくても動作する。

# Usage
## option
```
--config="config.yml"
    YAMLファイルを指定する

--port="8080"
    WebGUIのポートを指定する。
    ほぼ未実装。
    指定が無ければ起動しない。(ようにしたい)
```

## config
`destination`と`key`の指定には`command`か`text`が使用できる。
`forward`はkeyにローカルポート、valueにリモートホストとポートを指定する。
```
destination:
  command: |
    echo "admin@example.com"

key:
  text: ~/.ssh/key

forward:
  10002: example1.internal:8080
  10003: example2.internal:8080
```
