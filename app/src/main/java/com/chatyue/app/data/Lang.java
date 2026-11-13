package com.chatyue.app.data;

import android.content.Context;

/** 回答语言。只影响模型输出语言，不改界面文案（界面保持中文，避免半套翻译的割裂感）。 */
public final class Lang {

    public static final String[] NAMES = {"简体中文", "繁體中文", "English", "日本語", "한국어"};

    private Lang() {
    }

    public static String current(Context c) {
        String s = Prefs.language(c);
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(s)) return s;
        return NAMES[0];
    }

    public static void set(Context c, String name) {
        Prefs.setString(c, Prefs.KEY_LANG, name);
    }

    /** 追加到系统提示词的指令；简体中文不需要额外说明。 */
    public static String instruction(Context c) {
        String s = current(c);
        if (NAMES[0].equals(s)) return "";
        if (NAMES[1].equals(s)) return "请用繁體中文回答。";
        if (NAMES[2].equals(s)) return "Please answer in English.";
        if (NAMES[3].equals(s)) return "日本語で回答してください。";
        return "한국어로 답변해 주세요.";
    }
}
