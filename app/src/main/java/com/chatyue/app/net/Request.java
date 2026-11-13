package com.chatyue.app.net;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * 一次在途请求的句柄，负责「中断生成」。
 * 取消时同时断开底层连接与 wolfSSL 会话，保证 readLine 立刻返回。
 */
public class Request {

    private volatile boolean cancelled;
    private HttpURLConnection conn;
    private long nativeHandle;
    private Conn activeConn;

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
        HttpURLConnection c = conn;
        if (c != null) {
            try {
                c.disconnect();
            } catch (Throwable ignored) {
            }
        }
        Conn ac = activeConn;
        if (ac != null) ac.closeQuietly();
        long h = nativeHandle;
        if (h != 0) {
            try {
                WolfSsl.close(h);
            } catch (Throwable ignored) {
            }
        }
    }

    public void bind(HttpURLConnection c) {
        conn = c;
        if (cancelled && c != null) {
            try {
                c.disconnect();
            } catch (Throwable ignored) {
            }
        }
    }

    public void bindNative(long handle) {
        nativeHandle = handle;
        if (cancelled && handle != 0) {
            try {
                WolfSsl.close(handle);
            } catch (Throwable ignored) {
            }
        }
    }

    public void bindConn(Conn c) {
        activeConn = c;
        if (cancelled && c != null) c.closeQuietly();
    }

    /** HTTP 连接 + 响应流 + 状态码的统一封装。 */
    public static final class Conn {
        public int code;
        public java.io.BufferedReader reader;
        public java.io.InputStream raw;
        public String contentType = "";

        public void closeQuietly() {
            try {
                if (reader != null) reader.close();
            } catch (IOException ignored) {
            }
            try {
                if (raw != null) raw.close();
            } catch (IOException ignored) {
            }
        }
    }
}
