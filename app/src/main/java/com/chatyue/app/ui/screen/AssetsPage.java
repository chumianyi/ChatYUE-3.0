package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Assistants;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.PromptLib;
import com.chatyue.app.data.QuickMessages;
import com.chatyue.app.data.Skills;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;

/** 资源中心：把「助手资源」里散落的东西收在一个入口。 */
public class AssetsPage extends BasePage {

    public AssetsPage(MainActivity a) {
        super(a, "助手资源", "收藏 · 知识 · 模板");
        build();
    }

    private void build() {
        resetContent();
        heading("你的素材库", "这些都存在本机，不会同步到任何服务器。");

        CardList card = card();
        card.addRow(item(Icons.STAR, "收藏", "跨会话收集的回复", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new FavoritePage(act));
            }
        }));
        card.addRow(item(Icons.BOOK, "知识库", Knowledge.all(act).size() + " 条片段", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new KnowledgePage(act));
            }
        }));
        card.addRow(item(Icons.EDIT, "快捷消息", QuickMessages.all(act).size() + " 条", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new QuickMessagePage(act));
            }
        }));
        card.addRow(item(Icons.SPARKLE, "提示词库", PromptLib.all().size() + " 个模板", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new PromptLibraryPage(act));
            }
        }));
        card.addRow(item(Icons.ROBOT, "技能", Skills.enabledCount(act) + " / " + Skills.all().length + " 已开启",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        act.push(new SkillPage(act));
                    }
                }));
        card.addRow(item(Icons.USER, "助手", Assistants.all(act).size() + " 个角色", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new AssistantListPage(act));
            }
        }));

        section("说明");
        CardList tip = newCard(0);
        tip.padding(16, 14);
        tip.add(new Label(act, "知识库与技能会作为提示词的一部分随请求发送；收藏、快捷消息、提示词模板只在本机使用。",
                12.4f, Palette.INK_2, false));

        primaryButton("用量统计", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new UsagePage(act));
            }
        });
    }

    private MenuRow item(int icon, String title, String value, View.OnClickListener l) {
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(icon, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(title);
        r.value(value);
        r.chevron(true);
        r.setOnClickListener(l);
        return r;
    }
}
