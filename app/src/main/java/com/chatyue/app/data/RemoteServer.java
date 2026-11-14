package com.chatyue.app.data;

import android.content.Context;

import com.chatyue.app.net.HttpTransport;
import com.chatyue.app.net.Request;
import com.chatyue.app.net.Tls;
import com.chatyue.app.net.WolfSsl;
import com.chatyue.app.net.WolfTransport;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 远程 SSL（HTTPS）命令端点。
 *
 * 协议：
 *   POST {serverUrl}
 *   Authorization: Bearer {token}
 *   Content-Type: application/json
 *   {"command":"ls -la","cwd":"/root","timeout":30}
 *
 * 返回：
 *   {"ok":true,"exit":0,"output":"...","cwd":"/root","timeout":false}
 *
 * 普通证书走应用已有的 HttpTransport；平台 TLS 握手失败时自动切内置 wolfSSL，
 * 因此低版本 Android 也能连接现代 HTTPS 服务器。自签名证书单独走用户显式开启的
 * 跳过校验分支，仅用于内网测试。
 */
public final class RemoteServer {

    public static final class Result {
        public final int exit;
        public final String output;
        public final boolean timeout;

        Result(int e, String o, boolean t) {
            exit = e;
            output = o == null ? "" : o;
            timeout = t;
        }

        public boolean ok() {
            return !timeout && exit == 0;
        }
    }

    private RemoteServer() {
    }

    public static String endpoint(Context c) {
        String u = Prefs.serverUrl(c).trim();
        if (u.length() == 0) return "";
        if (u.indexOf("://") < 0) u = "https://" + u;
        return u;
    }

    public static Result exec(Context c, String command, String cwd, int timeoutMs) {
        if (!Prefs.serverEnabled(c)) return new Result(2, "服务器连接未启用", false);
        String url = endpoint(c);
        String token = Prefs.serverToken(c);
        if (url.length() == 0 || token.length() == 0) {
            return new Result(2, "请先在右上角「配置服务器连接」填写地址和令牌", false);
        }
        if (!url.startsWith("https://")) {
            return new Result(2, "服务器地址必须以 https:// 开头（本功能只连接 SSL 服务器）", false);
        }
        if (command == null || command.trim().length() == 0) {
            return new Result(2, "命令为空", false);
        }
        int timeoutSec = Math.max(1, timeoutMs / 1000);
        String body = buildBody(command, cwd, timeoutSec);
        byte[] payload;
        try {
            payload = body.getBytes("UTF-8");
        } catch (Throwable t) {
            return new Result(-1, "请求编码失败：" + t.getMessage(), false);
        }
        if (Prefs.serverSelfSigned(c)) {
            return execTrustAll(url, token, payload);
        }
        return execWithFallback(url, token, payload);
    }

    public static Result test(Context c) {
        return exec(c, "echo CHATYUE_OK", Prefs.serverCwd(c), 15000);
    }

    // ---------------------------------------------------------------- 正常证书链路

    private static Result execWithFallback(String url, String token, byte[] payload) {
        Request req = new Request();
        Request.Conn conn = null;
        try {
            try {
                conn = HttpTransport.open(url, token, payload, req);
            } catch (Throwable platform) {
                if (Tls.isTlsError(platform) && WolfSsl.isAvailable()) {
                    // 老系统 JSSE 无法握手时，用内置 wolfSSL 再试一次。
                    conn = WolfTransport.open(url, token, payload, req);
                } else {
                    throw platform;
                }
            }
            String text = read(conn.raw);
            return parse(conn.code, text);
        } catch (Throwable t) {
            return new Result(-1, t.getClass().getSimpleName() + "：" + t.getMessage(), false);
        } finally {
            if (conn != null) conn.closeQuietly();
            req.cancel();
        }
    }

    // ---------------------------------------------------------------- 自签名证书

    private static Result execTrustAll(String url, String token, byte[] payload) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(30000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("User-Agent", "ChatYUE/3.0 (Android)");
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) conn;
                https.setSSLSocketFactory(trustAllContext().getSocketFactory());
                https.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            }
            OutputStream out = conn.getOutputStream();
            try {
                out.write(payload);
                out.flush();
            } finally {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
            return parse(conn.getResponseCode(), read(conn));
        } catch (Throwable t) {
            return new Result(-1, t.getClass().getSimpleName() + "：" + t.getMessage(), false);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String buildBody(String command, String cwd, int timeoutSec) {
        JSONObject body = new JSONObject();
        try {
            body.put("command", command);
            body.put("cwd", cwd == null || cwd.trim().length() == 0 ? "/" : cwd);
            body.put("timeout", timeoutSec);
        } catch (Throwable ignored) {
        }
        return body.toString();
    }

    private static Result parse(int code, String text) {
        if (code < 200 || code >= 300) {
            return new Result(code, "HTTP " + code + "\n" + text, false);
        }
        try {
            JSONObject obj = new JSONObject(text);
            int exit = obj.optInt("exit", obj.optBoolean("ok", false) ? 0 : 1);
            boolean timed = obj.optBoolean("timeout", false);
            String output = obj.optString("output", text);
            return new Result(exit, output, timed);
        } catch (Throwable ignored) {
            return new Result(0, text, false);
        }
    }

    private static String read(InputStream in) {
        if (in == null) return "";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                if (buffer.size() >= 64 * 1024) break;
                buffer.write(buf, 0, Math.min(n, 64 * 1024 - buffer.size()));
            }
            return buffer.toString("UTF-8");
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + "：" + t.getMessage();
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static String read(HttpURLConnection conn) {
        try {
            int code = conn.getResponseCode();
            return read(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + "：" + t.getMessage();
        }
    }

    private static SSLContext trustAllContext() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }}, new SecureRandom());
        return sc;
    }
}
