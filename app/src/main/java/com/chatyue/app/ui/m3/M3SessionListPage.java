package com.chatyue.app.ui.m3;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;

import java.util.ArrayList;

/** 经典主题会话列表：搜索框 + 连续圆角列表 + 长按/按钮删除。 */
public class M3SessionListPage extends Page {

    private LinearLayout list;
    private Label count;
    private String filter = "";

    public M3SessionListPage(MainActivity a) {
        super(a);
        setBackgroundColor(M3.BG);

        M3TopBar bar = new M3TopBar(a, "会话", "所有本地对话", Icons.BACK, Icons.PLUS);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        bar.right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openNewChat();
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

        M3SearchField search = new M3SearchField(a, "搜索标题或内容");
        search.listener(new M3SearchField.Listener() {
            public void onText(String text) {
                filter = text == null ? "" : text.trim();
                render();
            }
        });
        body.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(50)));

        count = new Label(a, "", 11.8f, M3.ON_SURFACE_3, false);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(12);
        clp.leftMargin = Ui.dp(4);
        body.addView(count, clp);

        BounceScrollView scroll = new BounceScrollView(a);
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Ui.dp(10), 0, Ui.dp(30));
        scroll.addView(list, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        body.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        render();
    }

    @Override
    public void onPageShown() {
        render();
    }

    private void render() {
        list.removeAllViews();
        ArrayList<Session> all = SessionStore.list(act);
        int shown = 0;
        for (int i = 0; i < all.size(); i++) {
            Session s = all.get(i);
            if (filter.length() > 0) {
                String hay = (s.title + " " + s.preview()).toLowerCase();
                if (!hay.contains(filter.toLowerCase())) continue;
            }
            M3Card card = new M3Card(act);
            card.setRadius(22);
            card.pad(0, 2, 0, 2);
            M3Row row = new M3Row(act, false);
            row.icon(Icons.MESSAGE);
            row.title(s.title);
            row.desc(trim(s.preview(), 34));
            row.value(com.chatyue.app.util.TimeUtil.friendly(s.updatedAt));
            final Session session = s;
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Session full = SessionStore.load(act, session.id);
                    if (full != null) act.openChat(full);
                }
            });
            row.trailing(deleteButton(s));
            card.addView(row, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(10);
            list.addView(card, lp);
            shown++;
        }
        count.setText(filter.length() == 0
                ? "共 " + all.size() + " 段会话 · 仅存本机"
                : "找到 " + shown + " 段会话");
        if (shown == 0) {
            Label empty = new Label(act, filter.length() == 0 ? "还没有会话" : "没有匹配的会话",
                    18f, M3.ON_SURFACE, true);
            empty.setAlign(Label.CENTER);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            elp.topMargin = Ui.dp(60);
            list.addView(empty, elp);
            Label d = new Label(act, filter.length() == 0 ? "点右上角 + 开始第一段对话"
                    : "换个关键词试试", 12.4f, M3.ON_SURFACE_3, false);
            d.setAlign(Label.CENTER);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = Ui.dp(8);
            list.addView(d, dlp);
        }
    }

    private View deleteButton(final Session s) {
        IconButton del = new IconButton(act, Icons.TRASH, 0, M3.ON_SURFACE_3, 34, 16);
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                M3Dialog.confirmDanger(act.root().overlay, "删除这段对话？",
                        s.title + " 将被永久删除，无法恢复。", "删除", new Runnable() {
                            public void run() {
                                SessionStore.delete(act, s.id);
                                render();
                                act.toast("已删除");
                            }
                        });
            }
        });
        return del;
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        s = s.replace('\n', ' ');
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
