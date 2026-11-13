package com.chatyue.app.ui.screen;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.QuickMessages;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.Stepper;
import com.chatyue.app.ui.widget.Toggle;

/** 聊天设置：字号、记忆、输入与滚动的行为。 */
public class ChatSettingsPage extends BasePage {

    public ChatSettingsPage(MainActivity a) {
        super(a, "聊天", "输入 · 记忆 · 排版");

        heading("聊天的手感", "这些开关都立即生效，不用重开应用。");

        section("显示");
        stepperRow(Icons.DOC, "字号", "正文与代码块同步缩放", Prefs.fontPercent(act),
                85, 130, 5, "%", new Stepper.Listener() {
                    public void onValue(int value) {
                        Prefs.setFontPercent(act, value);
                    }
                });
        row(Icons.BRUSH, "外观与主题", "13 套配色（含经典 M3），即时生效", new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                act.push(new AppearancePage(act));
            }
        });

        section("行为");
        toggleRow(Icons.BOOK, "记忆", "参考历史消息与本机知识库", Prefs.getBool(act, Prefs.KEY_MEMORY, true),
                new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_MEMORY, checked);
                    }
                });
        toggleRow(Icons.SEND, "回车发送", "关闭后回车换行，用发送键提交",
                Prefs.getBool(act, Prefs.KEY_ENTER_SEND, true), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_ENTER_SEND, checked);
                    }
                });
        toggleRow(Icons.ARROW_UP, "跟随最新消息", "生成过程中自动滚到底部",
                Prefs.getBool(act, Prefs.KEY_AUTOSCROLL, true), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_AUTOSCROLL, checked);
                    }
                });
        stepperRow(Icons.CLOCK, "上下文轮数", "每次请求携带的历史消息条数", Prefs.contextRounds(act),
                2, 40, 2, " 条", new Stepper.Listener() {
                    public void onValue(int value) {
                        Prefs.setInt(act, Prefs.KEY_CONTEXT, value);
                    }
                });

        section("内容");
        rowWithValue(Icons.BOOK, "知识库", Knowledge.all(act).size() + " 条片段",
                new android.view.View.OnClickListener() {
                    public void onClick(android.view.View v) {
                        act.push(new KnowledgePage(act));
                    }
                });
        rowWithValue(Icons.EDIT, "快捷消息", QuickMessages.all(act).size() + " 条",
                new android.view.View.OnClickListener() {
                    public void onClick(android.view.View v) {
                        act.push(new QuickMessagePage(act));
                    }
                });
        rowWithValue(Icons.ROBOT, "模型参数", "温度 " + (Prefs.temperature(act) / 100f)
                        + " · 上限 " + Prefs.maxTokens(act),
                new android.view.View.OnClickListener() {
                    public void onClick(android.view.View v) {
                        act.push(new ModelParamsPage(act));
                    }
                });

        note("上下文轮数越大，回答越连续，消耗也越高；记忆关闭时只发送本轮问题。");

        CardList tip = newCard(18);
        tip.padding(16, 14);
        Label l = new Label(act,
                "提示：长按任意一条回复，可以复制、收藏、重新生成或查看纯文本。",
                12.4f, Palette.INK_2, false);
        tip.add(l);
    }
}
