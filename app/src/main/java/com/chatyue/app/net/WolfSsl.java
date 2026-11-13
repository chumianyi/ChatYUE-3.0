package com.chatyue.app.net;

/**
 * wolfSSL JNI 桥。.so 由 app/src/main/jni 下的 C 源码通过 ndk-build 生成，
 * 没有加载成功时 isAvailable() 返回 false，上层自动回落到平台 TLS。
 */
public final class WolfSsl {

    private static boolean sTried;
    private static boolean sLoaded;
    private static String sCaPath;

    private WolfSsl() {
    }

    public static synchronized boolean isAvailable() {
        if (!sTried) {
            sTried = true;
            try {
                System.loadLibrary("wolfssl_bridge");
                sLoaded = true;
            } catch (Throwable t) {
                sLoaded = false;
            }
        }
        return sLoaded;
    }

    public static void setCaBundle(String path) {
        sCaPath = path;
        if (isAvailable()) {
            try {
                nativeSetCaBundle(path);
            } catch (Throwable ignored) {
            }
        }
    }

    public static String caBundle() {
        return sCaPath;
    }

    public static native long connect(String host, int port, int timeoutMs);

    public static native int write(long handle, byte[] data, int off, int len);

    public static native int read(long handle, byte[] buf, int off, int len);

    public static native void close(long handle);

    public static native String lastError();

    public static native String version();

    private static native void nativeSetCaBundle(String path);
}
