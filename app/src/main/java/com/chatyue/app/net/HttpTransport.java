package com.chatyue.app.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 明文 HTTP / 平台 TLS 链路：全部走 HttpURLConnection。
 * 流式响应不在内存里攒完，拿到 InputStream 就立刻逐行读。
 */
public final class HttpTransport {

    private HttpTransport() {
    }

    public static Request.Conn open(String url, String apiKey, byte[] body, Request req) throws IOException {
        HttpURLConnection conn = null;
        URL u = new URL(url);
        conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(180000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream, application/json");
        conn.setRequestProperty("Accept-Encoding", "identity");
        conn.setRequestProperty("User-Agent", "ChatYUE/1.0 (Android; minSdk3)");
        conn.setRequestProperty("Connection", "keep-alive");
        if (apiKey != null && apiKey.length() > 0) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        req.bind(conn);
        if (req.isCancelled()) throw new IOException("已取消");

        OutputStream os = conn.getOutputStream();
        try {
            os.write(body);
            os.flush();
        } finally {
            try {
                os.close();
            } catch (IOException ignored) {
            }
        }

        int code = conn.getResponseCode();
        InputStream in;
        if (code >= 400) {
            in = conn.getErrorStream();
            if (in == null) in = conn.getInputStream();
        } else {
            in = conn.getInputStream();
        }
        Request.Conn c = new Request.Conn();
        c.code = code;
        c.raw = in;
        c.contentType = conn.getContentType() == null ? "" : conn.getContentType();
        c.reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), 4096);
        req.bindConn(c);
        return c;
    }
}
