package com.chatyue.app.ui.m3;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.md.MdText;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;

import java.util.ArrayList;

/** 经典主题收藏页：把各会话里标星的句子聚合起来。 */
public class M3FavoritePage extends Page {

    private static final class Entry {
        Session session;
        Message message;
    }

    private LinearLayout list;
    private Label count;

    public M3FavoritePage(MainActivity a) {
        super(a);
        setBackgroundColor(M3.BG);

        M3TopBar bar = new M3TopBar(a, "收藏", "留住想再看的句子", Icons.BACK, 0);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
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

        count = new Label(a, "", 12f, M3.ON_SURFACE_3, false);
        count.setPadding(Ui.dp(4), Ui.dp(4), 0, Ui.dp(8));
        body.addView(count, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        BounceScrollView scroll = new BounceScrollView(a);
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Ui.dp(4), 0, Ui.dp(30));
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
        ArrayList<Entry> entries = new ArrayList<Entry>();
        ArrayList<Session> all = SessionStore.list(act);
        for (int i = 0; i < all.size(); i++) {
            Session meta = all.get(i);
            Session full = SessionStore.load(act, meta.id);
            if (full == null) continue;
            for (int j = 0; j < full.messages.size(); j++) {
                Message m = full.messages.get(j);
                if (m.starred && m.text != null && m.text.trim().length() > 0) {
                    Entry e = new Entry();
                    e.session = full;
                    e.message = m;
                    entries.add(e);
                }
            }
        }
        count.setText(entries.size() == 0 ? "还没有收藏" : "共 " + entries.size() + " 条收藏 · 只存本机");
        if (entries.isEmpty()) {
            M3Card empty = new M3Card(act);
            empty.setRadius(24);
            empty.pad(20, 30, 20, 30);
            Label t = new Label(act, "还没有收藏的句子", 17f, M3.ON_SURFACE, true);
            t.setAlign(Label.CENTER);
            empty.addView(t, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            Label d = new Label(act, "在对话里长按一条消息，选择「收藏这条」就会出现在这里。",
                    12f, M3.ON_SURFACE_3, false);
            d.setAlign(Label.CENTER);
            d.setPadding(0, Ui.dp(9), 0, 0);
            empty.addView(d, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            list.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            final Entry e = entries.get(i);
            M3Card card = new M3Card(act);
            card.setRadius(22);
            card.pad(18, 15, 12, 15);

            LinearLayout top = new LinearLayout(act);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            Label s = new Label(act, e.session.title, 11.4f, M3.PRIMARY, true);
            top.addView(s, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            Label time = new Label(act, com.chatyue.app.util.TimeUtil.friendly(e.message.time),
                    9.8f, M3.ON_SURFACE_3, false);
            top.addView(time, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            IconButton star = new IconButton(act, Icons.STAR, 0, M3.PRIMARY, 30, 15);
            star.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    e.message.starred = false;
                    SessionStore.save(act, e.session);
                    render();
                    act.toast("已取消收藏");
                }
            });
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(Ui.dp(30), Ui.dp(30));
            slp.leftMargin = Ui.dp(4);
            top.addView(star, slp);
            card.addView(top, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            MdText text = new MdText(act, com.chatyue.app.md.MdInline.build(e.message.text,
                    M3.ON_SURFACE, M3.PRIMARY_DEEP), Ui.textPaint(M3.ON_SURFACE, 13.8f, false),
                    0, 0, 4f);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = Ui.dp(9);
            card.addView(text, tlp);

            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.openChat(e.session);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(10);
            list.addView(card, lp);
        }
    }
}
