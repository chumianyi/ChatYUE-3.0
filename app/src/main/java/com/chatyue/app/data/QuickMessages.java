package com.chatyue.app.data;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;

/** 快捷消息：对话页输入框上方 / 首页快捷入口使用，可在设置里增删。 */
public final class QuickMessages {

    private static final String[] DEFAULTS = {
            "用三点总结上面这段",
            "换个更口语的说法",
            "列一个可执行的步骤清单",
            "帮我想三个标题"
    };

    private QuickMessages() {
    }

    public static ArrayList<String> all(Context c) {
        ArrayList<String> out = new ArrayList<String>();
        String raw = Prefs.getString(c, Prefs.KEY_QUICK, "");
        if (raw == null || raw.length() == 0) {
            for (int i = 0; i < DEFAULTS.length; i++) out.add(DEFAULTS[i]);
            return out;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i, "");
                if (s != null && s.length() > 0) out.add(s);
            }
        } catch (Throwable ignored) {
        }
        if (out.isEmpty()) {
            for (int i = 0; i < DEFAULTS.length; i++) out.add(DEFAULTS[i]);
        }
        return out;
    }

    public static void save(Context c, ArrayList<String> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) arr.put(list.get(i));
        Prefs.setString(c, Prefs.KEY_QUICK, arr.toString());
    }

    public static void add(Context c, String text) {
        ArrayList<String> list = all(c);
        list.add(0, text);
        while (list.size() > 12) list.remove(list.size() - 1);
        save(c, list);
    }

    public static void remove(Context c, int index) {
        ArrayList<String> list = all(c);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            save(c, list);
        }
    }
}
