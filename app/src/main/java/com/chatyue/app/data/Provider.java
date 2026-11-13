package com.chatyue.app.data;

import org.json.JSONObject;

/**
 * 模型提供方配置。
 *
 * 内置的 ChatYUE 条目 Base URL / API Key 固化为常量，界面上不可见、不可编辑；
 * 第三方提供方由用户自行填写。
 */
public class Provider {

    public String id;
    public String name;
    public String baseUrl;
    public String apiKey;
    public String model;
    public boolean builtin;
    public int accent;

    public Provider() {
    }

    public boolean isBuiltin() {
        return builtin;
    }

    /** 拼接 /chat/completions，容忍末尾斜杠与已含路径两种写法。 */
    public String chatUrl() {
        String b = baseUrl == null ? "" : baseUrl.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("/chat/completions")) return b;
        if (b.endsWith("/v1")) return b + "/chat/completions";
        return b + "/v1/chat/completions";
    }

    public String toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("baseUrl", baseUrl);
            o.put("apiKey", apiKey);
            o.put("model", model);
            o.put("accent", accent);
        } catch (Exception ignored) {
        }
        return o.toString();
    }

    public static Provider fromJson(JSONObject o) {
        Provider p = new Provider();
        p.id = o.optString("id", Ids.next("p"));
        p.name = o.optString("name", "自定义模型");
        p.baseUrl = o.optString("baseUrl", "");
        p.apiKey = o.optString("apiKey", "");
        p.model = o.optString("model", "");
        p.accent = o.optInt("accent", 0);
        p.builtin = false;
        return p;
    }
}
