package com.chatyue.app.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Session {

    public String id;
    public String title;
    public long createdAt;
    public long updatedAt;
    /** 会话级系统提示词（角色卡 / Agent 人设），为空时使用全局设置。 */
    public String systemPrompt = "";
    public ArrayList<Message> messages = new ArrayList<Message>();

    public Session() {
        this.id = Ids.next("s");
        this.title = "新会话";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
    }

    public String preview() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            String t = m.text == null ? "" : m.text.trim();
            if (t.length() > 0) {
                return t.replace('\n', ' ');
            }
            if (m.imagePath != null) return "[图片]";
        }
        return "还没有内容";
    }

    public int userCount() {
        int n = 0;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).isUser()) n++;
        }
        return n;
    }

    /** 从首条用户消息自动生成标题。 */
    public void autoTitle() {
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (m.isUser() && m.text != null && m.text.trim().length() > 0) {
                String t = m.text.trim().replace('\n', ' ');
                if (t.length() > 18) t = t.substring(0, 18) + "…";
                title = t;
                return;
            }
        }
    }

    public String toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("title", title);
            o.put("createdAt", createdAt);
            o.put("updatedAt", updatedAt);
            o.put("systemPrompt", systemPrompt == null ? "" : systemPrompt);
            JSONArray arr = new JSONArray();
            for (int i = 0; i < messages.size(); i++) {
                arr.put(new JSONObject(messages.get(i).toJson()));
            }
            o.put("messages", arr);
        } catch (JSONException ignored) {
        }
        return o.toString();
    }

    public static Session fromJson(String raw) {
        try {
            JSONObject o = new JSONObject(raw);
            Session s = new Session();
            s.id = o.optString("id", Ids.next("s"));
            s.title = o.optString("title", "新会话");
            s.createdAt = o.optLong("createdAt", System.currentTimeMillis());
            s.updatedAt = o.optLong("updatedAt", s.createdAt);
            s.systemPrompt = o.optString("systemPrompt", "");
            JSONArray arr = o.optJSONArray("messages");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject mo = arr.optJSONObject(i);
                    if (mo != null) s.messages.add(Message.fromJson(mo));
                }
            }
            return s;
        } catch (Exception e) {
            return null;
        }
    }
}
