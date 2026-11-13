package com.chatyue.app.ui.screen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.ChatYueApp;
import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Agents;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.Avatar;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.QuickAction;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.Segmented;
import com.chatyue.app.util.TimeUtil;

import java.util.ArrayList;

/**
 * 首页。三段式：欢迎语 → 最近会话 / Agent / 群聊 → 资料；底部悬浮快捷条。
 */
public class HomePage extends Page {

    private static final String[] TABS = {"任务", "Agent", "群聊"};

    private BounceScrollView scroll;
    private LinearLayout container;
    private Segmented segmented;
    private Label greeting;
    private int tab;

    public HomePage(MainActivity a) {
        super(a);

        LinearLayout main = new LinearLayout(a);
        main.setOrientation(LinearLayout.VERTICAL);
        addView(main, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        FrameLayout top = new FrameLayout(a);
        main.addView(top, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(64)));

        IconButton user = new IconButton(a, Icons.USER, 0xFFFFFFFF, Palette.INK_2, 38, 18);
        user.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openSettings();
            }
        });
        FrameLayout.LayoutParams ulp = new FrameLayout.LayoutParams(Ui.dp(38), Ui.dp(38),
                Gravity.LEFT | Gravity.CENTER_VERTICAL);
        ulp.leftMargin = Ui.dp(20);
        top.addView(user, ulp);

        segmented = new Segmented(a, TABS);
        segmented.setSelected(0, false);
        segmented.setListener(new Segmented.OnSelect() {
            public void onSelect(int index) {
                tab = index;
                render();
            }
        });
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(Ui.dp(216), Ui.dp(40),
                Gravity.CENTER);
        top.addView(segmented, slp);

        IconButton search = new IconButton(a, Icons.SEARCH, 0xFFFFFFFF, Palette.INK_2, 38, 18);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new SearchPage(act));
            }
        });
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(Ui.dp(38), Ui.dp(38),
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        rlp.rightMargin = Ui.dp(20);
        top.addView(search, rlp);

        scroll = new BounceScrollView(a);
        container = new LinearLayout(a);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(Ui.dp(20), 0, Ui.dp(20), Ui.dp(140));
        scroll.addView(container, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        main.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        addView(buildBottomBar(), bottomParams());
        render();
    }

    private FrameLayout.LayoutParams bottomParams() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(78), Gravity.BOTTOM);
        lp.leftMargin = Ui.dp(18);
        lp.rightMargin = Ui.dp(18);
        lp.bottomMargin = Ui.dp(18);
        return lp;
    }

    private View buildBottomBar() {
        RoundBox bar = new RoundBox(act);
        bar.setFill(Palette.CARD);
        bar.setRadius(30);
        bar.setShadow(Palette.SHADOW, 12);
        bar.setPadding(Ui.dp(8), Ui.dp(6), Ui.dp(8), Ui.dp(6));

        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        QuickAction clock = new QuickAction(act, Icons.CLOCK, "定时任务");
        clock.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                quickAsk("定时任务", "帮我把这件事拆成可执行的提醒计划：");
            }
        });
        QuickAction note = new QuickAction(act, Icons.EDIT, "快速记录");
        note.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                quickAsk("快速记录", "帮我整理并归纳这段记录：");
            }
        });
        QuickAction trans = new QuickAction(act, Icons.TRANSLATE, "快速翻译");
        trans.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                quickAsk("快速翻译", "把下面内容翻译成地道的英文，保留语气：\n");
            }
        });
        row.addView(clock, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.FILL_PARENT, 1f));
        row.addView(note, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.FILL_PARENT, 1f));
        row.addView(trans, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.FILL_PARENT, 1f));

        QuickAction add = new QuickAction(act, Icons.PLUS, "新任务");
        add.colors(0xFFFFFFFF, 0xFFFFFFFF, Palette.PINK).radius(24);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openNewChat();
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(Ui.dp(88),
                LinearLayout.LayoutParams.FILL_PARENT);
        alp.leftMargin = Ui.dp(2);
        row.addView(add, alp);
        return bar;
    }

    private void quickAsk(String title, String preset) {
        act.openNewChatWithDraft(title, preset);
    }

    // ------------------------------------------------------------------ 渲染

    @Override
    public void onPageShown() {
        render();
    }

    @Override
    public void onDataChanged() {
        render();
    }

    private void render() {
        container.removeAllViews();
        if (tab == 0) renderTask();
        else if (tab == 1) renderAgents();
        else renderGroup();
    }

    private void renderTask() {
        greeting = new Label(act, TimeUtil.greeting(), 27f, Palette.INK, true);
        container.addView(greeting, lp(0, 26, 0, 0));
        container.addView(text(TimeUtil.dateLine(), 13f, Palette.INK_3, false), lp(0, 8, 0, 0));
        container.addView(text("今天还没聊什么，想到什么随时找我。", 14.6f, Palette.INK_2, false), lp(0, 6, 0, 0));

        container.addView(buildAvatarRow(), lp(0, 20, 0, 0));

        LinearLayout recentHead = new LinearLayout(act);
        recentHead.setOrientation(LinearLayout.HORIZONTAL);
        recentHead.setGravity(Gravity.CENTER_VERTICAL);
        recentHead.addView(sectionTitle("最近"), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Label all = new Label(act, "全部会话", 12f, Palette.PINK_DEEP, false);
        all.setPadding(0, Ui.dp(4), 0, Ui.dp(4));
        all.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new SessionListPage(act));
            }
        });
        recentHead.addView(all, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(recentHead, lp(0, 26, 0, 10));
        container.addView(buildRecentCard(), lp(0, 0, 0, 10));

        container.addView(sectionTitle("资料"), lp(0, 24, 0, 10));
        container.addView(buildKnowledgeCard(), lp(0, 0, 0, 10));
    }

    private View buildAvatarRow() {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        Avatar av = new Avatar(act);
        av.setBitmap(ChatYueApp.logo(act));
        av.setRing(Palette.PINK_SOFT, 3);
        col.addView(av, new LinearLayout.LayoutParams(Ui.dp(58), Ui.dp(58)));
        Label name = new Label(act, "ChatYUE", 11.6f, Palette.INK_2, false);
        name.setAlign(Label.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(Ui.dp(64),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = Ui.dp(5);
        col.addView(name, nlp);
        col.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openLatestOrNew();
            }
        });
        row.addView(col, new LinearLayout.LayoutParams(Ui.dp(64), LinearLayout.LayoutParams.WRAP_CONTENT));

        IconButton plus = new IconButton(act, Icons.PLUS, 0xFFFFFFFF, Palette.INK, 46, 20);
        plus.setStroke(1f, Palette.LINE);
        plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openNewChat();
            }
        });
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(Ui.dp(46), Ui.dp(46));
        plp.leftMargin = Ui.dp(18);
        row.addView(plus, plp);
        return row;
    }

    private View buildRecentCard() {
        ArrayList<Session> list = SessionStore.list(act);
        final Session latest = list.isEmpty() ? null : SessionStore.load(act, list.get(0).id);

        RoundBox card = cardBox();
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        View dot = new View(act) {
            protected void onDraw(Canvas canvas) {
                Paint p = Ui.fillPaint(latest == null ? Palette.INK_4 : Palette.PINK);
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, Ui.dpf(4.6f), p);
            }
        };
        row.addView(dot, new LinearLayout.LayoutParams(Ui.dp(16), Ui.dp(16)));

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(10);
        row.addView(col, clp);

        String title = latest == null ? "暂无会话" : latest.title;
        String sub = latest == null ? "点击下方 + 号开始新对话"
                : latest.preview() + " · " + TimeUtil.friendly(latest.updatedAt);
        col.addView(text(title, 15.4f, Palette.INK, true), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Label subLabel = text(sub, 12.2f, Palette.INK_3, false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(4);
        col.addView(subLabel, slp);

        View chev = new View(act) {
            protected void onDraw(Canvas canvas) {
                Icons.draw(canvas, Icons.CHEVRON_RIGHT, getWidth() / 2f, getHeight() / 2f,
                        Ui.dpf(15), Palette.INK_4, 1.8f);
            }
        };
        row.addView(chev, new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18)));

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (latest == null) act.openNewChat();
                else act.openChat(latest);
            }
        });
        return card;
    }

    private View buildKnowledgeCard() {
        RoundBox card = cardBox();
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        View icon = new View(act) {
            protected void onDraw(Canvas canvas) {
                Paint bg = Ui.fillPaint(Palette.BG);
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f, bg);
                Icons.draw(canvas, Icons.BOOK, getWidth() / 2f, getHeight() / 2f,
                        Ui.dpf(18), Palette.INK_2, 1.7f);
            }
        };
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(38), Ui.dp(38)));

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(12);
        row.addView(col, clp);
        col.addView(text("知识库", 15.4f, Palette.INK, true), matchWrap());
        ArrayList<String> notes = Knowledge.all(act);
        Label sub = text(notes.isEmpty() ? "点击添加文档" : notes.size() + " 条资料片段",
                12.2f, Palette.INK_3, false);
        LinearLayout.LayoutParams slp = matchWrap();
        slp.topMargin = Ui.dp(4);
        col.addView(sub, slp);

        View chev = new View(act) {
            protected void onDraw(Canvas canvas) {
                Icons.draw(canvas, Icons.CHEVRON_RIGHT, getWidth() / 2f, getHeight() / 2f,
                        Ui.dpf(15), Palette.INK_4, 1.8f);
            }
        };
        row.addView(chev, new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18)));

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openKnowledge();
            }
        });
        return card;
    }

    private void openKnowledge() {
        final Sheet sheet = new Sheet(act).title("知识库").subtitle("片段会随每次提问一起发送");
        ArrayList<String> notes = Knowledge.all(act);
        for (int i = 0; i < notes.size(); i++) {
            final String note = notes.get(i);
            final int index = i;
            String preview = note.length() > 18 ? note.substring(0, 18) + "…" : note;
            sheet.addDesc(Icons.DOC, preview, "点按删除", new Runnable() {
                public void run() {
                    Knowledge.remove(act, index);
                    render();
                    act.toast("已删除");
                }
            });
        }
        sheet.add(Icons.PLUS, "添加资料片段", new Runnable() {
            public void run() {
                Dialog.input(act.root().overlay, "添加资料片段", "例如：我的项目代号是…", "",
                        new Dialog.InputCallback() {
                            public void onInput(String text) {
                                Knowledge.add(act, text);
                                render();
                                act.toast("已加入知识库");
                            }
                        });
            }
        });
        sheet.show(act.root().overlay);
    }

    private void renderAgents() {
        container.addView(new Label(act, "Agent", 27f, Palette.INK, true), lp(0, 26, 0, 0));
        container.addView(text("选一个人设，直接开始。", 14.6f, Palette.INK_2, false), lp(0, 6, 0, 16));

        ArrayList<Agents.Agent> list = Agents.all();
        for (int i = 0; i < list.size(); i++) {
            final Agents.Agent a = list.get(i);
            RoundBox card = cardBox();
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(14), Ui.dp(16));
            card.addView(row, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

            View icon = new View(act) {
                protected void onDraw(Canvas canvas) {
                    Paint bg = Ui.fillPaint(Palette.PINK_SOFT);
                    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f, bg);
                    Icons.draw(canvas, a.icon, getWidth() / 2f, getHeight() / 2f,
                            Ui.dpf(19), Palette.PINK_DEEP, 1.8f);
                }
            };
            row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(42), Ui.dp(42)));

            LinearLayout col = new LinearLayout(act);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            clp.leftMargin = Ui.dp(12);
            row.addView(col, clp);
            col.addView(text(a.name, 15.4f, Palette.INK, true), matchWrap());
            Label d = text(a.desc, 12.2f, Palette.INK_3, false);
            LinearLayout.LayoutParams dlp = matchWrap();
            dlp.topMargin = Ui.dp(4);
            col.addView(d, dlp);

            View chev = new View(act) {
                protected void onDraw(Canvas canvas) {
                    Icons.draw(canvas, Icons.CHEVRON_RIGHT, getWidth() / 2f, getHeight() / 2f,
                            Ui.dpf(15), Palette.INK_4, 1.8f);
                }
            };
            row.addView(chev, new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18)));

            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.openNewChat(a.name, a.prompt);
                }
            });
            container.addView(card, lp(0, 0, 0, 12));
        }
    }

    private void renderGroup() {
        container.addView(new Label(act, "群聊", 27f, Palette.INK, true), lp(0, 26, 0, 0));
        container.addView(text("让三个角色同坐一桌，各自发言。", 14.6f, Palette.INK_2, false), lp(0, 6, 0, 16));

        RoundBox card = cardBox();
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(14), Ui.dp(16));
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        View icon = new View(act) {
            protected void onDraw(Canvas canvas) {
                Paint bg = Ui.fillPaint(Palette.PINK_SOFT);
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f, bg);
                Icons.draw(canvas, Icons.USER, getWidth() / 2f, getHeight() / 2f - Ui.dpf(4),
                        Ui.dpf(15), Palette.PINK_DEEP, 1.7f);
                Icons.draw(canvas, Icons.USER, getWidth() / 2f + Ui.dpf(7),
                        getHeight() / 2f + Ui.dpf(5), Ui.dpf(15), Palette.PINK_DEEP, 1.7f);
            }
        };
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(42), Ui.dp(42)));

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(12);
        row.addView(col, clp);
        col.addView(text("圆桌讨论", 15.4f, Palette.INK, true), matchWrap());
        Label d = text("务实派 / 反方 / 收尾人，三种视角轮流发言", 12.2f, Palette.INK_3, false);
        LinearLayout.LayoutParams dlp = matchWrap();
        dlp.topMargin = Ui.dp(4);
        col.addView(d, dlp);

        View chev = new View(act) {
            protected void onDraw(Canvas canvas) {
                Icons.draw(canvas, Icons.CHEVRON_RIGHT, getWidth() / 2f, getHeight() / 2f,
                        Ui.dpf(15), Palette.INK_4, 1.8f);
            }
        };
        row.addView(chev, new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18)));

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openNewChat("圆桌讨论",
                        "你是一个三人圆桌。每次回答依次以【务实派】【反方】【收尾人】三个身份发言，"
                                + "每个身份不超过四句，最后用一句话给出结论。不要跳出角色。");
            }
        });
        container.addView(card, lp(0, 0, 0, 12));

        container.addView(text("群聊记录会作为同一会话持续保存，随时可以回到这里继续。",
                12.4f, Palette.INK_3, false), lp(0, 6, 0, 0));
    }

    // ------------------------------------------------------------------ 工具

    private Label text(String s, float size, int color, boolean bold) {
        return new Label(act, s, size, color, bold);
    }

    private Label sectionTitle(String s) {
        return new Label(act, s, 14.2f, Palette.INK_2, false);
    }

    private RoundBox cardBox() {
        RoundBox box = new RoundBox(act);
        box.setFill(Palette.CARD);
        box.setRadius(18);
        box.setShadow(Palette.SHADOW, 8);
        box.setClickable(true);
        return box;
    }

    private LinearLayout.LayoutParams lp(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = Ui.dp(l);
        p.topMargin = Ui.dp(t);
        p.rightMargin = Ui.dp(r);
        p.bottomMargin = Ui.dp(b);
        return p;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void openLatestOrNew() {
        Session s = SessionStore.latest(act);
        if (s == null) act.openNewChat();
        else act.openChat(s);
    }

    private void searchSessions() {
        Dialog.input(act.root().overlay, "搜索会话", "输入关键词", "", new Dialog.InputCallback() {
            public void onInput(String text) {
                if (text == null || text.length() == 0) return;
                ArrayList<Session> all = SessionStore.list(act);
                Sheet sheet = new Sheet(act).title("搜索结果").subtitle("关键词：" + text);
                int hits = 0;
                for (int i = 0; i < all.size() && hits < 8; i++) {
                    Session meta = all.get(i);
                    Session full = SessionStore.load(act, meta.id);
                    final Session target = full;
                    boolean match = meta.title != null && meta.title.indexOf(text) >= 0;
                    if (!match && full != null) {
                        for (int k = 0; k < full.messages.size(); k++) {
                            String t = full.messages.get(k).text;
                            if (t != null && t.indexOf(text) >= 0) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (!match) continue;
                    hits++;
                    sheet.addDesc(Icons.DOT, meta.title,
                            TimeUtil.friendly(meta.updatedAt) + " · " + meta.preview(),
                            new Runnable() {
                                public void run() {
                                    if (target != null) act.openChat(target);
                                }
                            });
                }
                if (hits == 0) sheet.add(Icons.SEARCH, "没有匹配的会话", null);
                sheet.show(act.root().overlay);
            }
        });
    }
}
