package com.chatyue.app.data;

import android.content.Context;

/** Agent 能力的本地开关，最终拼成系统提示词里的行为约束。 */
public final class AgentFlags {

    private AgentFlags() {
    }

    public static boolean web(Context c) {
        return Prefs.getBool(c, Prefs.KEY_AGENT_WEB, true);
    }

    public static boolean code(Context c) {
        return Prefs.getBool(c, Prefs.KEY_AGENT_CODE, true);
    }

    public static boolean proactive(Context c) {
        return Prefs.getBool(c, Prefs.KEY_AGENT_PROACTIVE, false);
    }

    public static boolean collab(Context c) {
        return Prefs.getBool(c, Prefs.KEY_AGENT_COLLAB, false);
    }

    public static String compose(Context c) {
        StringBuilder sb = new StringBuilder();
        if (web(c)) {
            sb.append("- 你无法实时联网。涉及最新信息时说明这一点，并给出可直接使用的检索式。\n");
        }
        if (code(c)) {
            sb.append("- 涉及代码时给出可运行的完整实现，标明语言与依赖。\n");
        }
        if (proactive(c)) {
            sb.append("- 回答末尾可以用一句话给出下一步建议，不要长篇追问。\n");
        }
        if (collab(c)) {
            sb.append("- 复杂任务先分别从「实现者 / 审阅者」两个视角给出判断，再汇总结论。\n");
        }
        return sb.length() == 0 ? "" : "行为约束：\n" + sb.toString();
    }

    public static int enabledCount(Context c) {
        int n = 0;
        if (web(c)) n++;
        if (code(c)) n++;
        if (proactive(c)) n++;
        if (collab(c)) n++;
        return n;
    }
}
