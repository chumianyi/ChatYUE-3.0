package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.QuickMessages;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.EmptyState;
import com.chatyue.app.ui.widget.MenuRow;

import java.util.ArrayList;

/** 快捷消息管理：出现在对话页输入框上方，一键填入。 */
public class QuickMessagePage extends BasePage {

    public QuickMessagePage(MainActivity a) {
        super(a, "快捷消息", "一键填入输入框");
        build();
    }

    private void build() {
        resetContent();
        heading("常说的话，存起来", "对话页输入框上方会显示这些句子，点一下就能用它提问。");
        space(8);

        row(Icons.PLUS, "新增快捷消息", "最多 12 条", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.input(act.root().overlay, "新增快捷消息", "一句话就好", "",
                        new Dialog.InputCallback() {
                            public void onInput(String text) {
                                if (text == null || text.trim().length() == 0) return;
                                QuickMessages.add(act, text.trim());
                                build();
                                act.toast("已添加");
                            }
                        });
            }
        });

        ArrayList<String> list = QuickMessages.all(act);
        section("共 " + list.size() + " 条");
        if (list.isEmpty()) {
            content.addView(new EmptyState(act, Icons.EDIT, "还没有快捷消息", "加几句常用提问，对话时省一半打字。"),
                    lp(0, 0, 0, 0));
            return;
        }

        CardList card = card();
        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            final String text = list.get(i);
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(false, false);
            r.icon(Icons.EDIT, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(text);
            r.chevron(true);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Sheet s = new Sheet(act).title("快捷消息");
                    s.add(Icons.COPY, "复制", new Runnable() {
                        public void run() {
                            act.copyText(text);
                        }
                    });
                    s.addDanger(Icons.TRASH, "删除", new Runnable() {
                        public void run() {
                            QuickMessages.remove(act, index);
                            build();
                        }
                    });
                    s.show(act.root().overlay);
                }
            });
            card.addRow(r);
        }
        note("点对话页输入框上方的胶囊即可直接填入，不会自动发送。");
    }
}
