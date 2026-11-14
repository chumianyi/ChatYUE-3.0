#!/usr/bin/env python3
# ChatYUE 3.0 远程服务器参考端点（Python 3，仅标准库）。
#
# 这是给用户自己的服务器用的，不会被打包进 APK。
# 只监听本机或内网，必须用 HTTPS；请不要把无令牌的端点暴露到公网。
#
# 用法：
#   export CHATYUE_TOKEN='一串足够长的随机字符串'
#   openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
#     -keyout key.pem -out cert.pem -subj /CN=localhost
#   python3 chatyue_remote_server.py --cert cert.pem --key key.pem
#
# 默认监听 127.0.0.1:8765，路径 /v1/exec。

import argparse
import json
import os
import signal
import ssl
import subprocess
import sys
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

MAX_BODY = 1024 * 1024
MAX_OUTPUT = 60000
TOKEN = os.environ.get("CHATYUE_TOKEN", "")


class Handler(BaseHTTPRequestHandler):
    server_version = "ChatYUERemote/1.0"

    def log_message(self, fmt, *args):
        sys.stderr.write("[chatyue] " + (fmt % args) + "\n")

    def _json(self, code, obj):
        data = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        if self.path != "/v1/exec":
            self._json(404, {"ok": False, "output": "not found"})
            return
        auth = self.headers.get("Authorization", "")
        if not TOKEN or auth != "Bearer " + TOKEN:
            self._json(401, {"ok": False, "output": "unauthorized"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            length = 0
        if length <= 0 or length > MAX_BODY:
            self._json(400, {"ok": False, "output": "bad body"})
            return
        try:
            body = json.loads(self.rfile.read(length).decode("utf-8"))
            command = body.get("command", "")
            cwd = body.get("cwd", "/")
            timeout = int(body.get("timeout", 30))
        except Exception as e:
            self._json(400, {"ok": False, "output": "bad json: " + str(e)})
            return
        if not command or len(command) > 20000:
            self._json(400, {"ok": False, "output": "empty command"})
            return
        if not isinstance(cwd, str) or not os.path.isabs(cwd) or not os.path.isdir(cwd):
            self._json(400, {"ok": False, "output": "bad cwd"})
            return
        timeout = max(1, min(timeout, 120))
        timed_out = False
        try:
            proc = subprocess.Popen(
                command,
                shell=True,
                cwd=cwd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                preexec_fn=os.setsid,
            )
            try:
                out, _ = proc.communicate(timeout=timeout)
            except subprocess.TimeoutExpired:
                timed_out = True
                try:
                    os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
                except Exception:
                    proc.kill()
                out, _ = proc.communicate()
            text = (out or b"").decode("utf-8", "replace")
            if len(text) > MAX_OUTPUT:
                text = text[:MAX_OUTPUT] + "\n…（输出过长已截断）"
            self._json(200, {
                "ok": not timed_out and proc.returncode == 0,
                "exit": proc.returncode if proc.returncode is not None else -1,
                "output": text,
                "cwd": cwd,
                "timeout": timed_out,
            })
        except Exception as e:
            self._json(200, {"ok": False, "exit": -1,
                             "output": type(e).__name__ + ": " + str(e)})


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--cert", required=True)
    parser.add_argument("--key", required=True)
    args = parser.parse_args()
    if not TOKEN:
        sys.exit("请先设置 CHATYUE_TOKEN 环境变量")
    httpd = HTTPServer((args.host, args.port), Handler)
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.load_cert_chain(args.cert, args.key)
    httpd.socket = ctx.wrap_socket(httpd.socket, server_side=True)
    print("ChatYUE remote endpoint: https://%s:%d/v1/exec" % (args.host, args.port))
    httpd.serve_forever()


if __name__ == "__main__":
    main()
