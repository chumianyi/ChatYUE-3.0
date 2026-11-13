package com.chatyue.app.util;

import android.content.Context;

/**
 * 剪贴板兼容。
 *
 * API 1 只有 android.text.ClipboardManager；API 11 起换成 android.content.ClipboardManager + ClipData。
 * 这里不对新类做任何静态引用（连 import 都不写），全部走反射：
 * 老设备的类校验器不会因为一个在本机根本不存在的类而拒绝加载本类。
 */
public final class ClipboardCompat {

    private ClipboardCompat() {
    }

    public static void setText(Context c, String text) {
        if (c == null) return;
        String t = text == null ? "" : text;
        if (sdkInt() >= 11 && setWithClipData(c, t)) return;
        try {
            android.text.ClipboardManager cm = (android.text.ClipboardManager)
                    c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setText(t);
        } catch (Throwable ignored) {
        }
    }

    public static String getText(Context c) {
        if (c == null) return "";
        if (sdkInt() >= 11) {
            String s = getWithClipData(c);
            if (s != null) return s;
        }
        try {
            android.text.ClipboardManager cm = (android.text.ClipboardManager)
                    c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasText()) return cm.getText().toString();
        } catch (Throwable ignored) {
        }
        return "";
    }

    /** 反射调用 ClipData.newPlainText + ClipboardManager.setPrimaryClip。 */
    private static boolean setWithClipData(Context c, String text) {
        try {
            Object cm = c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return false;
            Class cd = Class.forName("android.content.ClipData");
            Object clip = cd.getMethod("newPlainText",
                    new Class[]{CharSequence.class, CharSequence.class})
                    .invoke(null, new Object[]{text, text});
            cm.getClass().getMethod("setPrimaryClip", new Class[]{cd})
                    .invoke(cm, new Object[]{clip});
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String getWithClipData(Context c) {
        try {
            Object cm = c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return null;
            Object clip = cm.getClass().getMethod("getPrimaryClip").invoke(cm, new Object[0]);
            if (clip == null) return null;
            Object item = clip.getClass().getMethod("getItemAt", new Class[]{Integer.TYPE})
                    .invoke(clip, new Object[]{Integer.valueOf(0)});
            if (item == null) return null;
            Object cs = item.getClass().getMethod("getText").invoke(item, new Object[0]);
            return cs == null ? null : cs.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    // 用 API 1 就存在的 Build.VERSION.SDK（字符串）判断版本，避免引用 API 4 的 SDK_INT
    private static int sdkInt() {
        try {
            return Integer.parseInt(android.os.Build.VERSION.SDK);
        } catch (Throwable t) {
            return 3;
        }
    }
}
