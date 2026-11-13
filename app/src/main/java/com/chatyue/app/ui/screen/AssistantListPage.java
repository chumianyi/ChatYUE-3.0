package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Assistants;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.MenuRow;

import java.util.ArrayList;

/** 助手列表：内置 4 个 + 自定义。点进去可改人设、设为默认。 */
public class AssistantListPage extends BasePage {

    public AssistantListPage(MainActivity a) {
        super(a, "助手与角色", "默认：" + Assistants.current(a).name);
        build();
    }

    private void build() {
        resetContent();
        heading("换一种相处方式", "每个助手都有自己的系统提示词与开场白，新建会话时自动带上。");

        ArrayList<Assistants.Assistant> list = Assistants.all(act);
        String def = Assistants.defaultId(act);
        CardList card = card();
        for (int i = 0; i < list.size(); i++) {
            final Assistants.Assistant as = list.get(i);
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(false, false);
            r.letter(as.letter, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(as.name);
            r.desc(as.desc);
            r.checked(as.id.equals(def));
            r.chevron(true);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    open(as);
                }
            });
            card.addRow(r);
        }

        note("勾选的是当前默认助手。内置助手不能被删除，但可以查看它的提示词。");

        primaryButton("新建助手", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new AssistantEditPage(act, null));
            }
        });
    }

    private void open(final Assistants.Assistant as) {
        Sheet s = new Sheet(act).title(as.name).subtitle(as.desc);
        if (!as.id.equals(Assistants.defaultId(act))) {
            s.add(Icons.CHECK, "设为默认", new Runnable() {
                public void run() {
                    Assistants.setDefault(act, as.id);
                    act.toast("默认助手：" + as.name);
                    build();
                }
            });
        }
        s.add(Icons.EDIT, as.builtin ? "查看提示词" : "编辑", new Runnable() {
            public void run() {
                act.push(new AssistantEditPage(act, as));
            }
        });
        if (!as.builtin) {
            s.addDanger(Icons.TRASH, "删除", new Runnable() {
                public void run() {
                    Assistants.remove(act, as.id);
                    build();
                    act.toast("已删除");
                }
            });
        }
        s.show(act.root().overlay);
    }
}
