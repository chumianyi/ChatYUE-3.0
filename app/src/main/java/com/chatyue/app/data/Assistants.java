package com.chatyue.app.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 助手（人设）。内置 4 个不可删除，用户可新建/编辑/删除自定义助手，
 * 并可把任意一个设为默认，新建会话时自动带上它的系统提示词与开场白。
 */
public final class Assistants {

    public static class Assistant {
        public String id;
        public String name = "";
        public String desc = "";
        public String prompt = "";
        public String greeting = "";
        public String letter = "助";
        public boolean builtin;

        public Assistant(String id, String name, String desc, String prompt, String greeting, String letter) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.prompt = prompt;
            this.greeting = greeting;
            this.letter = letter;
        }
    }

    public static final String ID_GENERAL = "a_general";
    public static final String ID_WRITER = "a_writer";
    public static final String ID_TRANSLATOR = "a_translator";
    public static final String ID_CODER = "a_coder";

    private Assistants() {
    }

    public static ArrayList<Assistant> all(Context c) {
        ArrayList<Assistant> list = new ArrayList<Assistant>();
        list.add(new Assistant(ID_GENERAL, "全能助手", "通用对话、问答、分析与执行",
                "你是 ChatYUE 的全能助手。回答要准确、结构清晰、可直接执行；不确定时明确说明，不要编造。",
                "你好，我是 ChatYUE。有什么想做的，直接说就行。", "助"));
        list.add(new Assistant(ID_WRITER, "文字编辑", "改写、润色、总结、扩写",
                "你是一位中文文字编辑。保持原意，优化结构与节奏；除非被要求，不要增加原文没有的事实。",
                "把文字发给我，我来改。", "文"));
        list.add(new Assistant(ID_TRANSLATOR, "翻译官", "中英互译，保留语气",
                "你是专业翻译。中英互译时保留原文语气与术语一致性，只输出译文，必要时再补一句注释。",
                "发我一段话，我按原语气翻译。", "译"));
        list.add(new Assistant(ID_CODER, "代码搭子", "写代码、读代码、排错误",
                "你是资深工程师。先给可运行的完整实现，再解释关键点；遇到报错先定位根因，不要猜。",
                "贴代码或报错，我来看。", "码"));
        list.addAll(custom(c));
        return list;
    }

    public static ArrayList<Assistant> custom(Context c) {
        ArrayList<Assistant> out = new ArrayList<Assistant>();
        String raw = Prefs.getString(c, Prefs.KEY_ASSISTANTS, "");
        if (raw == null || raw.length() == 0) return out;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Assistant a = new Assistant(
                        o.optString("id", Ids.next("a")),
                        o.optString("name", "助手"),
                        o.optString("desc", ""),
                        o.optString("prompt", ""),
                        o.optString("greeting", ""),
                        o.optString("letter", "助"));
                out.add(a);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    public static Assistant byId(Context c, String id) {
        ArrayList<Assistant> list = all(c);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) return list.get(i);
        }
        return list.get(0);
    }

    public static String defaultId(Context c) {
        String id = Prefs.getString(c, Prefs.KEY_ASSISTANT, ID_GENERAL);
        return id == null || id.length() == 0 ? ID_GENERAL : id;
    }

    public static void setDefault(Context c, String id) {
        Prefs.setString(c, Prefs.KEY_ASSISTANT, id);
    }

    public static Assistant current(Context c) {
        return byId(c, defaultId(c));
    }

    /** 新增或覆盖（按 id 匹配）。内置助手的修改不落盘，避免污染默认设定。 */
    public static void save(Context c, Assistant a) {
        ArrayList<Assistant> list = custom(c);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(a.id)) {
                list.set(i, a);
                found = true;
                break;
            }
        }
        if (!found) list.add(a);
        JSONArray arr = new JSONArray();
        try {
            for (int i = 0; i < list.size(); i++) {
                Assistant x = list.get(i);
                JSONObject o = new JSONObject();
                o.put("id", x.id);
                o.put("name", x.name);
                o.put("desc", x.desc);
                o.put("prompt", x.prompt);
                o.put("greeting", x.greeting);
                o.put("letter", x.letter);
                arr.put(o);
            }
        } catch (Throwable ignored) {
        }
        Prefs.setString(c, Prefs.KEY_ASSISTANTS, arr.toString());
    }

    public static void remove(Context c, String id) {
        ArrayList<Assistant> list = custom(c);
        JSONArray arr = new JSONArray();
        try {
            for (int i = 0; i < list.size(); i++) {
                Assistant x = list.get(i);
                if (x.id.equals(id)) continue;
                JSONObject o = new JSONObject();
                o.put("id", x.id);
                o.put("name", x.name);
                o.put("desc", x.desc);
                o.put("prompt", x.prompt);
                o.put("greeting", x.greeting);
                o.put("letter", x.letter);
                arr.put(o);
            }
        } catch (Throwable ignored) {
        }
        Prefs.setString(c, Prefs.KEY_ASSISTANTS, arr.toString());
        if (defaultId(c).equals(id)) setDefault(c, ID_GENERAL);
    }
}
