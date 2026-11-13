package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.AgentFlags;
import com.chatyue.app.data.Agents;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.Toggle;

import java.util.ArrayList;

/** Agent：能力开关 + 人设入口。开关直接改变系统提示词里的行为约束。 */
public class AgentPage extends BasePage {

    public AgentPage(MainActivity a) {
        super(a, "Agent", AgentFlags.enabledCount(a) + " 项能力已开启");
        build();
    }

    private void build() {
        resetContent();
        heading("让它更像一个助手", "这些开关不会改变模型本身，只改变我要求它怎么做事。");

        section("能力");
        toggleRow(Icons.GLOBE, "联网意识", "承认无法实时联网，并给出检索式",
                Prefs.getBool(act, Prefs.KEY_AGENT_WEB, true), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_AGENT_WEB, checked);
                        bar.setSubtitle(AgentFlags.enabledCount(act) + " 项能力已开启");
                    }
                });
        toggleRow(Icons.DOC, "代码优先", "先给完整实现，再解释",
                Prefs.getBool(act, Prefs.KEY_AGENT_CODE, true), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_AGENT_CODE, checked);
                        bar.setSubtitle(AgentFlags.enabledCount(act) + " 项能力已开启");
                    }
                });
        toggleRow(Icons.SPARKLE, "主动建议", "回答末尾给一句下一步",
                Prefs.getBool(act, Prefs.KEY_AGENT_PROACTIVE, false), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_AGENT_PROACTIVE, checked);
                        bar.setSubtitle(AgentFlags.enabledCount(act) + " 项能力已开启");
                    }
                });
        toggleRow(Icons.ROBOT, "双视角复核", "复杂任务先实现后审阅",
                Prefs.getBool(act, Prefs.KEY_AGENT_COLLAB, false), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_AGENT_COLLAB, checked);
                        bar.setSubtitle(AgentFlags.enabledCount(act) + " 项能力已开启");
                    }
                });
        note("约束会追加在系统提示词之后。模型偶尔会忽略，这不是本地逻辑能强行保证的。");

        section("人设");
        ArrayList<Agents.Agent> list = Agents.all();
        CardList card = card();
        for (int i = 0; i < list.size(); i++) {
            final Agents.Agent ag = list.get(i);
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(false, false);
            r.icon(ag.icon, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(ag.name);
            r.desc(ag.desc);
            r.chevron(true);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.openNewChat(ag.name, ag.prompt);
                }
            });
            card.addRow(r);
        }

        primaryButton("管理助手与角色", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new AssistantListPage(act));
            }
        });
        note("人设页的助手会出现在新建会话的选择里，也可以设成默认。");
    }
}
