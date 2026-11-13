package com.chatyue.app.util;

/**
 * 自实现 Base64。android.util.Base64 是 API 8 才有的，低版本必须自己算。
 */
public final class Base64Codec {

    private static final char[] TAB =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private Base64Codec() {
    }

    public static String encode(byte[] data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder((data.length + 2) / 3 * 4);
        int i = 0;
        while (i + 2 < data.length) {
            int n = ((data[i] & 0xff) << 16) | ((data[i + 1] & 0xff) << 8) | (data[i + 2] & 0xff);
            sb.append(TAB[(n >>> 18) & 0x3f]);
            sb.append(TAB[(n >>> 12) & 0x3f]);
            sb.append(TAB[(n >>> 6) & 0x3f]);
            sb.append(TAB[n & 0x3f]);
            i += 3;
        }
        int rest = data.length - i;
        if (rest == 1) {
            int n = (data[i] & 0xff) << 16;
            sb.append(TAB[(n >>> 18) & 0x3f]);
            sb.append(TAB[(n >>> 12) & 0x3f]);
            sb.append("==");
        } else if (rest == 2) {
            int n = ((data[i] & 0xff) << 16) | ((data[i + 1] & 0xff) << 8);
            sb.append(TAB[(n >>> 18) & 0x3f]);
            sb.append(TAB[(n >>> 12) & 0x3f]);
            sb.append(TAB[(n >>> 6) & 0x3f]);
            sb.append('=');
        }
        return sb.toString();
    }

    public static byte[] decode(String s) {
        if (s == null) return new byte[0];
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '=' || indexOf(ch) >= 0) clean.append(ch);
        }
        while (clean.length() % 4 != 0) clean.append('=');
        int pad = 0;
        if (clean.length() > 0 && clean.charAt(clean.length() - 1) == '=') pad++;
        if (clean.length() > 1 && clean.charAt(clean.length() - 2) == '=') pad++;
        byte[] out = new byte[clean.length() / 4 * 3 - pad];
        int oi = 0;
        for (int i = 0; i < clean.length(); i += 4) {
            int n = 0;
            for (int k = 0; k < 4; k++) {
                char ch = clean.charAt(i + k);
                int v = ch == '=' ? 0 : indexOf(ch);
                n = (n << 6) | (v < 0 ? 0 : v);
            }
            if (oi < out.length) out[oi++] = (byte) ((n >>> 16) & 0xff);
            if (oi < out.length) out[oi++] = (byte) ((n >>> 8) & 0xff);
            if (oi < out.length) out[oi++] = (byte) (n & 0xff);
        }
        return out;
    }

    private static int indexOf(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return -1;
    }
}
