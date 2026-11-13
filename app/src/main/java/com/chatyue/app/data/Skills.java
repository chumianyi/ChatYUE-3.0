package com.chatyue.app.data;

import android.content.Context;

/**
 * 技能：一组可开关的能力描述，开启后会作为约束片段拼进系统提示词。
 * 低版本没有插件机制，这里用「提示词片段 + 本地开关」实现等效能力发现。
 */
public final class Skills {

    public static class Skill {
        public String id;
        public String name;
        public String desc;
        public String prompt;
        public int icon;

        public Skill(String id, String name, String desc, String prompt, int icon) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.prompt = prompt;
            this.icon = icon;
        }
    }

    private Skills() {
    }

    public static Skill[] all() {
        return new Skill[]{
                new Skill("summarize", "结构化总结", "长文先给结论，再列要点",
                        "当用户要求总结时，先给一句话结论，再给不超过五条要点。", 29),
                new Skill("steps", "步骤化执行", "任何任务都拆成可勾选步骤",
                        "遇到需要动手的任务，输出编号步骤，每步以动词开头。", 33),
                new Skill("code", "代码讲解", "给完整实现再讲关键点",
                        "给代码时先给可直接运行的完整实现，再解释关键设计与边界。", 24),
                new Skill("polish", "中文润色", "去掉翻译腔与冗余",
                        "改写中文时去掉翻译腔、避免「首先其次最后」的模板句式。", 34),
                new Skill("socratic", "反问澄清", "信息不足时先问三个问题",
                        "当需求缺少关键信息时，先提不超过三个澄清问题，再给初步方案。", 27),
                new Skill("brief", "极简回答", "能一句说清就不写两段",
                        "默认用最短篇幅回答；只有用户要求展开时才写长。", 36)
        };
    }

    public static boolean enabled(Context c, String id) {
        String raw = Prefs.getString(c, Prefs.KEY_SKILLS, "");
        if (raw == null || raw.length() == 0) return false;
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().equals(id)) return true;
        }
        return false;
    }

    public static void setEnabled(Context c, String id, boolean on) {
        String raw = Prefs.getString(c, Prefs.KEY_SKILLS, "");
        StringBuilder sb = new StringBuilder();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i].trim();
                if (p.length() == 0 || p.equals(id)) continue;
                if (sb.length() > 0) sb.append(',');
                sb.append(p);
            }
        }
        if (on) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        Prefs.setString(c, Prefs.KEY_SKILLS, sb.toString());
    }

    /** 拼成一段约束，追加在系统提示词之后。 */
    public static String compose(Context c) {
        Skill[] list = all();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            if (enabled(c, list[i].id)) {
                if (sb.length() > 0) sb.append('\n');
                sb.append("- ").append(list[i].prompt);
            }
        }
        return sb.toString();
    }

    public static int enabledCount(Context c) {
        int n = 0;
        Skill[] list = all();
        for (int i = 0; i < list.length; i++) if (enabled(c, list[i].id)) n++;
        return n;
    }
}
