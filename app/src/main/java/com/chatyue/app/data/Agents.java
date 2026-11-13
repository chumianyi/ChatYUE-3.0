package com.chatyue.app.data;

import com.chatyue.app.core.Icons;

import java.util.ArrayList;

/** 内置 Agent 人设。点击即开一个带专属系统提示词的新会话。 */
public final class Agents {

    public static class Agent {
        public String name;
        public String desc;
        public String prompt;
        public int icon;

        public Agent(String n, String d, String p, int i) {
            name = n;
            desc = d;
            prompt = p;
            icon = i;
        }
    }

    private Agents() {
    }

    public static ArrayList<Agent> all() {
        ArrayList<Agent> list = new ArrayList<Agent>();
        list.add(new Agent("写作助手", "结构、润色、长文续写",
                "你是一位中文写作教练，擅长结构化表达与润色。回答时先给出结构，再给出成稿；语言干净，不堆砌形容词。", Icons.EDIT));
        list.add(new Agent("代码助手", "审查、重构、解释原理",
                "你是一位资深 Android / Java 工程师。回答代码问题时先给结论，再给可运行的最小示例，并指出兼容性风险。", Icons.ROBOT));
        list.add(new Agent("翻译官", "中英互译，保留语气",
                "你是一位专业译者，擅长中英互译。翻译要忠实原意、保留语气与节奏，必要时给出一句术语说明。", Icons.TRANSLATE));
        list.add(new Agent("学习规划", "拆解目标，给出日程",
                "你是一位学习规划师。先澄清目标与可用时间，再给出可执行的周计划，标注每天的产出物。", Icons.STAR));
        return list;
    }
}
