package com.chatyue.app.data;

/**
 * 局部 ID 生成器。View.generateViewId() 要 API 17，这里自己发号。
 */
public final class Ids {

    private static int sSeq = 1000;

    private Ids() {
    }

    public static String next(String prefix) {
        sSeq++;
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + "_" + sSeq;
    }

    public static int viewId() {
        sSeq++;
        return 0x7f000000 + (sSeq & 0x00ffffff);
    }
}
