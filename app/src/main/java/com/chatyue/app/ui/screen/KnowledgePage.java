package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.EmptyState;
import com.chatyue.app.ui.widget.FieldBox;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.SoftButton;

import java.util.ArrayList;

/** 知识库：本机片段，开启记忆时随请求发送。 */
public class KnowledgePage extends BasePage {

    private FieldBox input;

    public KnowledgePage(MainActivity a) {
        super(a, "知识库", "随对话一起参考");
        build();
    }

    private void build() {
        resetContent();
        heading("记住这些事实", "术语、产品背景、固定口径…… 写进来，回答时会作为参考资料。");
        space(10);

        input = new FieldBox(act, "新增片段", "例如：我们的产品叫 ChatYUE，最低支持 Android 1.5", "", 3);
        content.addView(input, lp(16, 0, 16, 10));
        SoftButton add = new SoftButton(act, "添加片段", 13.6f, 0xFFFFFFFF, Palette.PINK);
        add.radius(14);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String t = input.text();
                if (t.length() == 0) {
                    act.toast("先写点内容");
                    return;
                }
                Knowledge.add(act, t);
                act.toast("已加入知识库");
                build();
            }
        });
        content.addView(add, lp(16, 0, 16, 4));

        ArrayList<String> list = Knowledge.all(act);
        section("共 " + list.size() + " 条");
        if (list.isEmpty()) {
            content.addView(new EmptyState(act, Icons.BOOK, "还没有片段",
                    "把一个事实写进来，之后每次提问都会带上它。"), lp(0, 0, 0, 0));
            note("片段不会上传到服务器，只在本机随请求正文一起发送。");
            return;
        }

        CardList card = card();
        for (int i = list.size() - 1; i >= 0; i--) {
            final int index = i;
            final String text = list.get(i);
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(false, false);
            r.icon(Icons.BOOK, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(text.length() > 30 ? text.substring(0, 30) + "…" : text);
            r.desc(text.length() > 30 ? text.substring(30, Math.min(70, text.length())) + "…" : "点击管理");
            r.chevron(true);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    menu(index, text);
                }
            });
            card.addRow(r);
        }
        note("点击任意条目可以编辑或删除。片段总长度会被截断到约 1200 字，避免挤占对话预算。");
    }

    private void menu(final int index, final String text) {
        Sheet s = new Sheet(act).title("片段").subtitle(text.length() > 40 ? text.substring(0, 40) + "…" : text);
        s.add(Icons.COPY, "复制内容", new Runnable() {
            public void run() {
                act.copyText(text);
            }
        });
        s.add(Icons.EDIT, "编辑", new Runnable() {
            public void run() {
                Dialog.input(act.root().overlay, "编辑片段", "内容", text, new Dialog.InputCallback() {
                    public void onInput(String value) {
                        if (value == null || value.trim().length() == 0) return;
                        Knowledge.set(act, index, value.trim());
                        build();
                        act.toast("已保存");
                    }
                });
            }
        });
        s.addDanger(Icons.TRASH, "删除", new Runnable() {
            public void run() {
                Knowledge.remove(act, index);
                build();
                act.toast("已删除");
            }
        });
        s.show(act.root().overlay);
    }
}
