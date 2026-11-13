package com.chatyue.app.data;

import java.util.ArrayList;

/**
 * 提示词模板库。纯静态数据，点击后填入输入框，不放任何「模板变量」的复杂机制。
 */
public final class PromptLib {

    public static class Prompt {
        public String cat;
        public String title;
        public String body;

        public Prompt(String cat, String title, String body) {
            this.cat = cat;
            this.title = title;
            this.body = body;
        }
    }

    public static final String[] CATS = {"写作", "工作", "学习", "生活", "编程"};

    private PromptLib() {
    }

    public static ArrayList<Prompt> all() {
        ArrayList<Prompt> list = new ArrayList<Prompt>();
        list.add(new Prompt("写作", "公众号开头", "帮我写三个能让人继续读下去的开头，主题是："));
        list.add(new Prompt("写作", "把稿子改短", "把下面这段压缩到一半篇幅，保留全部事实：\n"));
        list.add(new Prompt("写作", "起标题", "给这篇文章起 8 个标题，风格分别是：稳妥、悬念、数字、反差。正文：\n"));
        list.add(new Prompt("工作", "会议纪要", "把下面的会议记录整理成：结论 / 待办（负责人+时间）/ 风险：\n"));
        list.add(new Prompt("工作", "周报", "根据这些零散记录写一份周报，突出结果，不要流水账：\n"));
        list.add(new Prompt("工作", "邮件", "写一封得体的邮件，目的是：，收件人是："));
        list.add(new Prompt("学习", "费曼讲解", "用一个初中生能听懂的方式解释这个概念，并给一个生活类比："));
        list.add(new Prompt("学习", "出题", "根据下面的内容出 5 道选择题，并给出答案与解析：\n"));
        list.add(new Prompt("学习", "读书笔记", "把这段内容整理成三层：一句话结论 / 三条要点 / 一个可执行动作：\n"));
        list.add(new Prompt("生活", "旅行计划", "帮我排一个 3 天行程，城市是：，偏好：，预算："));
        list.add(new Prompt("生活", "菜谱", "用现有食材做两道菜，食材是：，厨具只有："));
        list.add(new Prompt("编程", "读代码", "解释这段代码做了什么，指出潜在 bug，并给出改法：\n"));
        list.add(new Prompt("编程", "写单测", "为下面的方法补单元测试，覆盖边界与异常：\n"));
        list.add(new Prompt("编程", "正则", "写一个正则，需求是：，并给三个匹配示例。"));
        return list;
    }

    public static ArrayList<String> byCat(String cat) {
        ArrayList<String> out = new ArrayList<String>();
        ArrayList<Prompt> all = all();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).cat.equals(cat)) out.add(all.get(i).title);
        }
        return out;
    }

    public static Prompt find(String title) {
        ArrayList<Prompt> all = all();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).title.equals(title)) return all.get(i);
        }
        return null;
    }
}
