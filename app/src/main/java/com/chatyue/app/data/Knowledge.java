package com.chatyue.app.data;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;

/**
 * 极简本地知识库：用户录入的片段会在开启记忆时拼进 system prompt。
 * 数据量刻意保持很小（默认上限 1600 字），避免把上下文撑爆。
 */
public final class Knowledge {

    private static final String KEY = "kb_notes";

    private Knowledge() {
    }

    public static ArrayList<String> all(Context c) {
        ArrayList<String> out = new ArrayList<String>();
        try {
            JSONArray arr = new JSONArray(Prefs.getString(c, KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i, "");
                if (s.length() > 0) out.add(s);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void add(Context c, String text) {
        if (text == null || text.trim().length() == 0) return;
        ArrayList<String> list = all(c);
        list.add(text.trim());
        write(c, list);
    }

    public static void remove(Context c, int index) {
        ArrayList<String> list = all(c);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            write(c, list);
        }
    }

    public static void set(Context c, int index, String text) {
        ArrayList<String> list = all(c);
        if (index >= 0 && index < list.size() && text != null && text.trim().length() > 0) {
            list.set(index, text.trim());
            write(c, list);
        }
    }

    private static void write(Context c, ArrayList<String> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) arr.put(list.get(i));
        Prefs.setString(c, KEY, arr.toString());
    }

    public static String compose(Context c, int maxChars) {
        ArrayList<String> list = all(c);
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("以下是我提供的资料片段，回答时优先参考：\n");
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (sb.length() + s.length() > maxChars) break;
            sb.append("- ").append(s.replace('\n', ' ')).append('\n');
        }
        return sb.length() > 30 ? sb.toString() : "";
    }
}
