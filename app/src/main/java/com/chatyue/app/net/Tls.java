package com.chatyue.app.net;

/** TLS 相关诊断：判断失败是否属于「老系统 TLS 栈太旧」，并给出面向用户的说明。 */
public final class Tls {

    private Tls() {
    }

    public static boolean isTlsError(Throwable t) {
        int depth = 0;
        while (t != null && depth < 8) {
            String name = t.getClass().getName();
            if (name.indexOf("SSL") >= 0 || name.indexOf("Cert") >= 0
                    || name.indexOf("Trust") >= 0 || name.indexOf("Handshake") >= 0) {
                return true;
            }
            String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (msg.indexOf("ssl") >= 0 || msg.indexOf("handshake") >= 0
                    || msg.indexOf("certificate") >= 0 || msg.indexOf("trust anchor") >= 0
                    || msg.indexOf("protocol") >= 0) {
                return true;
            }
            t = t.getCause();
            depth++;
        }
        return false;
    }

    public static String describe(Throwable t) {
        if (t == null) return "未知错误";
        String msg = t.getMessage();
        if (msg == null || msg.length() == 0) msg = t.getClass().getSimpleName();
        if (isTlsError(t)) {
            return "TLS 握手失败：本机 TLS 栈过旧（" + msg + "）";
        }
        if (msg.indexOf("timeout") >= 0 || msg.indexOf("Timeout") >= 0) {
            return "连接超时，请检查网络";
        }
        if (msg.indexOf("Unable to resolve host") >= 0 || msg.indexOf("UnknownHost") >= 0) {
            return "域名解析失败，请检查网络或代理";
        }
        if (msg.indexOf("refused") >= 0) {
            return "连接被拒绝，服务端可能未启动";
        }
        return msg;
    }
}
