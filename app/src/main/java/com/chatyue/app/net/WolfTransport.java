package com.chatyue.app.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * wolfSSL 兜底链路。
 *
 * 老系统的 JSSE 栈只支持到 TLS 1.0/1.1、证书库也早已过期，现代 HTTPS 站点基本无法握手。
 * 因此这里用内置的 wolfSSL（JNI）自己做 TLS 会话，然后在会话上手工收发 HTTP/1.1：
 * 请求行 + 头 + body 手写，响应按 chunked 或 content-length 流式解析。
 *
 * 只在平台链路握手失败时才走到这里，正常情况下仍然使用 HttpURLConnection。
 */
public final class WolfTransport {

    private WolfTransport() {
    }

    public static Request.Conn open(String url, String apiKey, byte[] body, Request req) throws IOException {
        if (!WolfSsl.isAvailable()) {
            throw new IOException("wolfSSL 兜底库未加载（jniLibs 中缺少 libwolfssl_bridge.so）");
        }
        URL u = new URL(url);
        if (!"https".equalsIgnoreCase(u.getProtocol())) {
            return HttpTransport.open(url, apiKey, body, req);
        }
        String host = u.getHost();
        int port = u.getPort() > 0 ? u.getPort() : 443;
        long h = WolfSsl.connect(host, port, 20000);
        if (h == 0) {
            throw new IOException("wolfSSL 握手失败：" + WolfSsl.lastError());
        }
        req.bindNative(h);
        if (req.isCancelled()) {
            WolfSsl.close(h);
            throw new IOException("已取消");
        }

        String path = u.getFile();
        if (path == null || path.length() == 0) path = "/";
        StringBuilder sb = new StringBuilder();
        sb.append("POST ").append(path).append(" HTTP/1.1\r\n");
        sb.append("Host: ").append(host);
        if (port != 443) sb.append(':').append(port);
        sb.append("\r\n");
        sb.append("Content-Type: application/json; charset=utf-8\r\n");
        sb.append("Accept: text/event-stream\r\n");
        sb.append("Accept-Encoding: identity\r\n");
        sb.append("User-Agent: ChatYUE/1.0 (wolfSSL)\r\n");
        if (apiKey != null && apiKey.length() > 0) {
            sb.append("Authorization: Bearer ").append(apiKey).append("\r\n");
        }
        sb.append("Content-Length: ").append(body.length).append("\r\n");
        sb.append("Connection: close\r\n\r\n");

        writeAll(h, sb.toString().getBytes("UTF-8"), req);
        writeAll(h, body, req);

        NativeInput in = new NativeInput(h);
        String statusLine = readLine(in);
        if (statusLine == null) throw new IOException("wolfSSL 未返回响应");
        int code = 0;
        try {
            String[] parts = statusLine.split(" ");
            if (parts.length >= 2) code = Integer.parseInt(parts[1].trim());
        } catch (Exception ignored) {
        }
        String contentType = "";
        boolean chunked = false;
        long contentLength = -1;
        String line;
        int guard = 0;
        while ((line = readLine(in)) != null && line.length() > 0 && guard++ < 200) {
            int idx = line.indexOf(':');
            if (idx <= 0) continue;
            String key = line.substring(0, idx).trim().toLowerCase();
            String value = line.substring(idx + 1).trim();
            if (key.equals("content-type")) contentType = value;
            if (key.equals("transfer-encoding") && value.toLowerCase().indexOf("chunked") >= 0) chunked = true;
            if (key.equals("content-length")) {
                try {
                    contentLength = Long.parseLong(value);
                } catch (Exception ignored) {
                }
            }
        }
        InputStream bodyStream = new BufferedInputStream(in, 8192);
        if (chunked) {
            bodyStream = new ChunkedInputStream(bodyStream);
        } else if (contentLength >= 0) {
            bodyStream = new LimitedInputStream(bodyStream, contentLength);
        }

        Request.Conn c = new Request.Conn();
        c.code = code;
        c.raw = bodyStream;
        c.contentType = contentType;
        c.reader = new BufferedReader(new InputStreamReader(bodyStream, "UTF-8"), 4096);
        req.bindConn(c);
        return c;
    }

    private static void writeAll(long h, byte[] data, Request req) throws IOException {
        int off = 0;
        while (off < data.length) {
            if (req.isCancelled()) throw new IOException("已取消");
            int n = WolfSsl.write(h, data, data.length - off, off);
            if (n <= 0) throw new IOException("wolfSSL 写入失败：" + WolfSsl.lastError());
            off += n;
        }
    }

    static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) >= 0) {
            if (b == '\n') break;
            if (b != '\r') bos.write(b);
            if (bos.size() > 65536) break;
        }
        if (b < 0 && bos.size() == 0) return null;
        return new String(bos.toByteArray(), "UTF-8");
    }

    static final class NativeInput extends InputStream {
        private final long handle;
        private final byte[] one = new byte[1];

        NativeInput(long h) {
            handle = h;
        }

        @Override
        public int read() throws IOException {
            int n = read(one, 0, 1);
            return n <= 0 ? -1 : (one[0] & 0xff);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len <= 0) return 0;
            int n = WolfSsl.read(handle, b, len, off);
            if (n <= 0) return -1;
            return n;
        }
    }

    /** HTTP/1.1 chunked 解码。 */
    static final class ChunkedInputStream extends InputStream {
        private final InputStream in;
        private int remain;
        private boolean eof;
        private boolean first;

        ChunkedInputStream(InputStream in) {
            this.in = in;
        }

        private boolean nextChunk() throws IOException {
            if (eof) return false;
            String line = readLine(in);
            if (line == null) {
                eof = true;
                return false;
            }
            line = line.trim();
            int semi = line.indexOf(';');
            if (semi > 0) line = line.substring(0, semi);
            if (line.length() == 0) return nextChunk();
            int size;
            try {
                size = Integer.parseInt(line, 16);
            } catch (Exception e) {
                eof = true;
                return false;
            }
            if (size == 0) {
                // 吃掉 trailer
                String t;
                while ((t = readLine(in)) != null && t.length() > 0) {
                    // ignore
                }
                eof = true;
                return false;
            }
            remain = size;
            first = true;
            return true;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int n = read(b, 0, 1);
            return n <= 0 ? -1 : (b[0] & 0xff);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len <= 0) return 0;
            if (remain == 0) {
                if (!nextChunk()) return -1;
            }
            int toRead = Math.min(len, remain);
            int n = in.read(b, off, toRead);
            if (n <= 0) return -1;
            remain -= n;
            if (remain == 0) {
                readLine(in);
            }
            return n;
        }
    }

    static final class LimitedInputStream extends InputStream {
        private final InputStream in;
        private long remain;

        LimitedInputStream(InputStream in, long len) {
            this.in = in;
            this.remain = len;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int n = read(b, 0, 1);
            return n <= 0 ? -1 : (b[0] & 0xff);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remain <= 0) return -1;
            int toRead = (int) Math.min(len, remain);
            int n = in.read(b, off, toRead);
            if (n <= 0) return -1;
            remain -= n;
            return n;
        }
    }
}
