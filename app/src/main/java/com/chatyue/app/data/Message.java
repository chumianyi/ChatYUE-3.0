package com.chatyue.app.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 一条消息。纯 POJO + 手写 JSON 序列化（低版本没有 Gson，也不引入依赖）。
 */
public class Message {

    public static final int ROLE_USER = 0;
    public static final int ROLE_ASSISTANT = 1;
    public static final int ROLE_SYSTEM = 2;

    public String id;
    public int role;
    public String text = "";
    public String reasoning = "";
    public String imagePath;
    public long time;
    public boolean error;
    public boolean stopped;
    /** 仅运行时有效：这一条正在流式生成。 */
    public boolean streaming;
    /** 是否已经结束（用于区分「空回复」与「正在生成」）。 */
    public boolean finished = true;
    /** 收藏标记：出现在「收藏」页里。 */
    public boolean starred;
    /** 3.0：用户随消息附上的任意文件（图片仍走 imagePath 参与视觉输入）。 */
    public ArrayList<String> attachments = new ArrayList<String>();
    /** 3.0：Agent 本轮产出的文件（绝对路径）。 */
    public ArrayList<String> artifacts = new ArrayList<String>();
    /** 3.0：多轮任务的过程记录，显示在气泡里的任务卡片。 */
    public String agentLog = "";
    /** 3.0：反思与修正的说明（没有则为空）。 */
    public String reflectionNote = "";
    /** 3.0：反思后重新生成的正文是否已合并进 text。 */
    public boolean reflected;

    public Message() {
    }

    public Message(int role, String text) {
        this.id = Ids.next("m");
        this.role = role;
        this.text = text == null ? "" : text;
        this.time = System.currentTimeMillis();
        this.finished = true;
    }

    public boolean isUser() {
        return role == ROLE_USER;
    }

    public String toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("role", role);
            o.put("text", text);
            o.put("reasoning", reasoning);
            o.put("image", imagePath == null ? "" : imagePath);
            o.put("time", time);
            o.put("error", error);
            o.put("stopped", stopped);
            o.put("starred", starred);
            o.put("agentLog", agentLog == null ? "" : agentLog);
            o.put("reflectionNote", reflectionNote == null ? "" : reflectionNote);
            o.put("reflected", reflected);
            if (!attachments.isEmpty()) {
                org.json.JSONArray arr = new org.json.JSONArray();
                for (int i = 0; i < attachments.size(); i++) arr.put(attachments.get(i));
                o.put("attachments", arr);
            }
            if (!artifacts.isEmpty()) {
                org.json.JSONArray arr = new org.json.JSONArray();
                for (int i = 0; i < artifacts.size(); i++) arr.put(artifacts.get(i));
                o.put("artifacts", arr);
            }
        } catch (JSONException ignored) {
        }
        return o.toString();
    }

    public static Message fromJson(JSONObject o) {
        Message m = new Message();
        m.id = o.optString("id", Ids.next("m"));
        m.role = o.optInt("role", ROLE_USER);
        m.text = o.optString("text", "");
        m.reasoning = o.optString("reasoning", "");
        String img = o.optString("image", "");
        m.imagePath = (img == null || img.length() == 0 || "null".equals(img)) ? null : img;
        m.time = o.optLong("time", System.currentTimeMillis());
        m.error = o.optBoolean("error", false);
        m.stopped = o.optBoolean("stopped", false);
        m.starred = o.optBoolean("starred", false);
        m.agentLog = o.optString("agentLog", "");
        m.reflectionNote = o.optString("reflectionNote", "");
        m.reflected = o.optBoolean("reflected", false);
        org.json.JSONArray att = o.optJSONArray("attachments");
        if (att != null) {
            for (int i = 0; i < att.length(); i++) {
                String p = att.optString(i, "");
                if (p.length() > 0) m.attachments.add(p);
            }
        }
        org.json.JSONArray art = o.optJSONArray("artifacts");
        if (art != null) {
            for (int i = 0; i < art.length(); i++) {
                String p = art.optString(i, "");
                if (p.length() > 0) m.artifacts.add(p);
            }
        }
        m.finished = true;
        return m;
    }
}
