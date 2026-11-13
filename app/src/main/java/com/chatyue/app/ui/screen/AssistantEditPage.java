package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.data.Assistants;
import com.chatyue.app.data.Ids;
import com.chatyue.app.ui.widget.FieldBox;

/** 助手编辑：名称、简介、系统提示词、开场白、头像字。 */
public class AssistantEditPage extends BasePage {

    private final Assistants.Assistant editing;
    private final boolean creating;

    public AssistantEditPage(MainActivity a, Assistants.Assistant assistant) {
        super(a, assistant == null ? "新建助手" : "编辑助手",
                assistant != null && assistant.builtin ? "内置助手只读" : "改完立即生效");
        editing = assistant;
        creating = assistant == null;

        Assistants.Assistant src = assistant == null
                ? new Assistants.Assistant(Ids.next("a"), "", "", "", "", "助")
                : assistant;

        heading(creating ? "造一个专属助手" : src.name,
                creating ? "写清它的身份、语气和边界，越具体越稳定。" : "内置助手的提示词可以看，但不建议改。");

        final FieldBox name = field("名称", "例如：论文搭子", src.name, 1);
        final FieldBox letter = field("头像字", "一个字，例如：论", src.letter, 1);
        final FieldBox desc = field("一句话简介", "例如：帮我改摘要、查引用格式", src.desc, 1);
        final FieldBox prompt = field("系统提示词", "你是一位…… 回答时……", src.prompt, 5);
        final FieldBox greeting = field("开场白", "可留空。对话开始时自动发送的一句", src.greeting, 2);

        boolean readonly = assistant != null && assistant.builtin;
        if (readonly) {
            name.edit.setEnabled(false);
            letter.edit.setEnabled(false);
            desc.edit.setEnabled(false);
            prompt.edit.setEnabled(false);
            greeting.edit.setEnabled(false);
        }

        note("提示词会与全局设定、画像、技能约束一起拼接，顺序是：助手 → 全局 → 画像 → 技能 → 语言。");

        if (!readonly) {
            primaryButton(creating ? "创建并设为默认" : "保存", new View.OnClickListener() {
                public void onClick(View v) {
                    String n = name.text();
                    if (n.length() == 0) {
                        act.toast("先给助手起个名字");
                        return;
                    }
                    Assistants.Assistant out = new Assistants.Assistant(
                            creating ? Ids.next("a") : editing.id,
                            n,
                            desc.text(),
                            prompt.text(),
                            greeting.text(),
                            letter.text().length() == 0 ? n.substring(0, 1) : letter.text().substring(0, 1));
                    Assistants.save(act, out);
                    if (creating) Assistants.setDefault(act, out.id);
                    act.toast(creating ? "已创建" : "已保存");
                    act.pop();
                }
            });
        }

        if (!creating && !editing.builtin) {
            ghostButton("删除这个助手", new View.OnClickListener() {
                public void onClick(View v) {
                    Assistants.remove(act, editing.id);
                    act.toast("已删除");
                    act.pop();
                }
            });
        }
    }
}
