package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.PromptLib;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.MenuRow;

import java.util.ArrayList;

/** 提示词模板库：按分类浏览，点一下带进新会话。 */
public class PromptLibraryPage extends BasePage {

    private String cat = PromptLib.CATS[0];
    private CardList listCard;

    public PromptLibraryPage(MainActivity a) {
        super(a, "提示词库", "选一个模板开始");
        build();
    }

    private void build() {
        resetContent();
        heading("不用从空白开始", "选一个模板，内容会填进输入框，你可以先改再发。");

        ArrayList<View> chips = new ArrayList<View>();
        for (int i = 0; i < PromptLib.CATS.length; i++) {
            final String c = PromptLib.CATS[i];
            final com.chatyue.app.ui.widget.SoftButton b = new com.chatyue.app.ui.widget.SoftButton(
                    act, c, 12.6f,
                    c.equals(cat) ? 0xFFFFFFFF : Palette.INK_2,
                    c.equals(cat) ? Palette.PINK : Palette.CARD);
            b.pad(14, 8).radius(13);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cat = c;
                    build();
                }
            });
            chips.add(b);
        }
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        android.widget.LinearLayout row = new android.widget.LinearLayout(act);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        for (int i = 0; i < chips.size(); i++) {
            android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) p.leftMargin = com.chatyue.app.core.Ui.dp(8);
            row.addView(chips.get(i), p);
        }
        hs.addView(row, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));
        content.addView(hs, lp(16, 4, 16, 12));

        ArrayList<PromptLib.Prompt> all = PromptLib.all();
        listCard = newCard(0);
        for (int i = 0; i < all.size(); i++) {
            final PromptLib.Prompt p = all.get(i);
            if (!p.cat.equals(cat)) continue;
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(false, false);
            r.icon(Icons.SPARKLE, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(p.title);
            r.desc(p.body.replace('\n', ' '));
            r.chevron(true);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    use(p);
                }
            });
            listCard.addRow(r);
        }

        note("模板只是起点。改掉冒号后面的内容，效果会好很多。");
    }

    private void use(final PromptLib.Prompt p) {
        Dialog.alert(act.root().overlay, p.title, p.body, "带进新会话", new Runnable() {
            public void run() {
                act.openNewChatWithDraft(p.body);
            }
        });
    }
}
