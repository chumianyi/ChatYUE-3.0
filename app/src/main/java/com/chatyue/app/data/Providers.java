package com.chatyue.app.data;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;

public final class Providers {

    public static final String BUILTIN_ID = "chatyue";
    public static final String BUILTIN_NAME = "ChatYUE";
    public static final String BUILTIN_BASE = "http://103.236.99.177:24512/v1";
    public static final String BUILTIN_KEY = "sk-123456789012345678901234567890";
    public static final String BUILTIN_MODEL = "GLM-4.1V-Thinking-Flash";

    private Providers() {
    }

    public static Provider builtin() {
        Provider p = new Provider();
        p.id = BUILTIN_ID;
        p.name = BUILTIN_NAME;
        p.baseUrl = BUILTIN_BASE;
        p.apiKey = BUILTIN_KEY;
        p.model = BUILTIN_MODEL;
        p.builtin = true;
        p.accent = 0;
        return p;
    }

    public static ArrayList<Provider> custom(Context c) {
        ArrayList<Provider> out = new ArrayList<Provider>();
        String raw = Prefs.getString(c, Prefs.KEY_CUSTOM_PROVIDERS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                if (arr.optJSONObject(i) != null) {
                    out.add(Provider.fromJson(arr.optJSONObject(i)));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static ArrayList<Provider> all(Context c) {
        ArrayList<Provider> out = new ArrayList<Provider>();
        out.add(builtin());
        out.addAll(custom(c));
        return out;
    }

    public static Provider find(Context c, String id) {
        if (id == null || id.length() == 0) return builtin();
        if (BUILTIN_ID.equals(id)) return builtin();
        ArrayList<Provider> list = custom(c);
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).id)) return list.get(i);
        }
        return builtin();
    }

    public static Provider current(Context c) {
        return find(c, Prefs.getString(c, Prefs.KEY_PROVIDER, BUILTIN_ID));
    }

    public static String currentId(Context c) {
        return current(c).id;
    }

    public static void setDefault(Context c, String id) {
        Prefs.setString(c, Prefs.KEY_PROVIDER, id);
    }

    public static void saveCustom(Context c, Provider p) {
        if (p == null || p.builtin) return;
        ArrayList<Provider> list = custom(c);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(p.id)) {
                list.set(i, p);
                found = true;
                break;
            }
        }
        if (!found) list.add(p);
        write(c, list);
    }

    public static void remove(Context c, String id) {
        if (BUILTIN_ID.equals(id)) return;
        ArrayList<Provider> list = custom(c);
        ArrayList<Provider> next = new ArrayList<Provider>();
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).id.equals(id)) next.add(list.get(i));
        }
        write(c, next);
        if (id.equals(Prefs.getString(c, Prefs.KEY_PROVIDER, BUILTIN_ID))) {
            setDefault(c, BUILTIN_ID);
        }
    }

    private static void write(Context c, ArrayList<Provider> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            try {
                arr.put(new org.json.JSONObject(list.get(i).toJson()));
            } catch (Exception ignored) {
            }
        }
        Prefs.setString(c, Prefs.KEY_CUSTOM_PROVIDERS, arr.toString());
    }

    /** 用于界面展示的脱敏串：只露前 6 位与后 4 位。 */
    public static String maskKey(String key) {
        if (key == null || key.length() == 0) return "未填写";
        if (key.length() <= 10) return "••••••";
        return key.substring(0, 6) + "••••" + key.substring(key.length() - 4);
    }
}
