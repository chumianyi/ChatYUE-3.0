# ChatYUE 3.0 远程服务器端点

这是给用户自己的服务器用的参考实现，不会打进 APK。

## 协议

```http
POST /v1/exec
Authorization: Bearer <CHATYUE_TOKEN>
Content-Type: application/json

{"command":"ls -la","cwd":"/root","timeout":30}
```

```json
{"ok":true,"exit":0,"output":"...","cwd":"/root","timeout":false}
```

## 启动

```bash
export CHATYUE_TOKEN='足够长的随机字符串'
openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -keyout key.pem -out cert.pem -subj /CN=localhost
python3 chatyue_remote_server.py --cert cert.pem --key key.pem
```

默认监听 `127.0.0.1:8765`。公网部署请换成正式证书，并在反向代理/防火墙层限制来源。

## 在应用里配置

右上角「配置服务器连接」填写：

- 服务器地址：`https://你的域名/v1/exec`
- 访问令牌：与 `CHATYUE_TOKEN` 相同
- 远程工作目录：如 `/root` 或 `/var/www`
- 自签名证书：只有使用自签测试证书时才打开

保存后点「保存并测试连接」，成功会执行 `echo CHATYUE_OK`。

> 这是受令牌保护、只应由你控制的服务器命令端点。不要把令牌提交到公开仓库。
