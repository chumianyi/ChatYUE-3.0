package com.chatyue.app.ui.m3;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;

import java.util.ArrayList;

/** 经典主题知识库：条目式卡片、右滑式删除按钮、M3 输入弹窗。 */
public class M3KnowledgePage extends Page {

    private LinearLayout list;
    private Label count;

    public M3KnowledgePage(MainActivity a) {
        super(a);
        setBackgroundColor(M3.BG);

        M3TopBar bar = new M3TopBar(a, "知识库", "对话时会自动参考", Icons.BACK, Icons.PLUS);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        bar.right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addEntry();
            }
        });
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(70)));

        LinearLayout body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(18), Ui.dp(2), Ui.dp(18), Ui.dp(24));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        blp.topMargin = Ui.dp(70);
        addView(body, blp);

        M3Card hero = new M3Card(a, M3.PRIMARY_CONTAINER, 0);
        hero.setRadius(24);
        hero.pad(18, 15, 18, 15);
        LinearLayout heroRow = new LinearLayout(a);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        IconButton ic = new IconButton(a, Icons.BOOK, M3.SURFACE, M3.PRIMARY_DEEP, 36, 18);
        heroRow.addView(ic, new LinearLayout.LayoutParams(Ui.dp(36), Ui.dp(36)));
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Label t = new Label(a, "本地知识片段", 14.2f, M3.ON_PRIMARY_CONTAINER, true);
        col.addView(t, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        count = new Label(a, "", 10.8f, Ui.alpha(M3.ON_PRIMARY_CONTAINER, 180), false);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(3);
        col.addView(count, clp);
        LinearLayout.LayoutParams clp2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp2.leftMargin = Ui.dp(12);
        heroRow.addView(col, clp2);
        hero.addView(heroRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        body.addView(hero, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        BounceScrollView scroll = new BounceScrollView(a);
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Ui.dp(14), 0, Ui.dp(30));
        scroll.addView(list, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f);
        body.addView(scroll, slp);

        render();
    }

    @Override
    public void onPageShown() {
        render();
    }

    private void addEntry() {
        M3Dialog.input(act.root().overlay, "新增知识片段", "比如：我的项目叫星河软件站", "",
                new M3Dialog.InputCallback() {
                    public void onInput(String text) {
                        if (text == null || text.length() == 0) return;
                        Knowledge.add(act, text);
                        render();
                        act.toast("已加入知识库");
                    }
                });
    }

    private void render() {
        list.removeAllViews();
        ArrayList<String> all = Knowledge.all(act);
        count.setText(all.size() + " 条 · 只保存在本机");
        if (all.isEmpty()) {
            M3Card empty = new M3Card(act);
            empty.setRadius(24);
            empty.pad(20, 28, 20, 28);
            Label t = new Label(act, "还没有知识片段", 16f, M3.ON_SURFACE, true);
            t.setAlign(Label.CENTER);
            empty.addView(t, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            Label d = new Label(act, "放入项目设定和写作偏好，对话会更贴合你。",
                    12f, M3.ON_SURFACE_3, false);
            d.setAlign(Label.CENTER);
            d.setPadding(0, Ui.dp(8), 0, 0);
            empty.addView(d, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            list.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            final int index = i;
            final String text = all.get(i);
            M3Card card = new M3Card(act);
            card.setRadius(22);
            card.pad(18, 15, 10, 15);
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            Label num = new Label(act, String.valueOf(i + 1), 12f, M3.PRIMARY, true);
            row.addView(num, new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24)));
            com.chatyue.app.ui.md.MdText body = new com.chatyue.app.ui.md.MdText(act, text,
                    Ui.textPaint(M3.ON_SURFACE, 13.6f, false), 0, 0, 4f);
            body.setPadding(0, 0, Ui.dp(6), 0);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(body, blp);
            IconButton more = new IconButton(act, Icons.MORE_H, 0, M3.ON_SURFACE_3, 30, 15);
            more.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    entryMenu(index, text);
                }
            });
            row.addView(more, new LinearLayout.LayoutParams(Ui.dp(30), Ui.dp(30)));
            card.addView(row, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    editEntry(index, text);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(9);
            list.addView(card, lp);
        }
    }

    private void entryMenu(final int index, final String text) {
        M3Sheet sheet = new M3Sheet(act).title("这条片段");
        sheet.add(Icons.EDIT, "编辑", new Runnable() {
            public void run() {
                editEntry(index, text);
            }
        });
        sheet.add(Icons.COPY, "复制", new Runnable() {
            public void run() {
                act.copyText(text);
            }
        });
        sheet.addDanger(Icons.TRASH, "删除", new Runnable() {
            public void run() {
                Knowledge.remove(act, index);
                render();
                act.toast("已删除");
            }
        });
        sheet.show(act.root().overlay);
    }

    private void editEntry(final int index, String text) {
        M3Dialog.input(act.root().overlay, "编辑片段", "", text, new M3Dialog.InputCallback() {
            public void onInput(String t) {
                if (t == null || t.length() == 0) return;
                Knowledge.set(act, index, t);
                render();
                act.toast("已保存");
            }
        });
    }
}
