package com.chatyue.app.ui.m3;

import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;

import java.util.ArrayList;

/**
 * 经典主题首页：M3 大标题、主色 hero 卡、三张快捷卡、连续列表卡和药丸式底栏。
 * 这套页面与默认主题的 HomePage 完全不共享布局代码。
 */
public class M3HomePage extends Page {

    private LinearLayout content;
    private BounceScrollView scroll;
    private M3NavBar nav;

    public M3HomePage(MainActivity a) {
        super(a);
        setBackgroundColor(M3.BG);

        M3TopBar bar = new M3TopBar(a, "ChatYUE", com.chatyue.app.util.TimeUtil.dateLine(),
                Icons.USER, Icons.SEARCH);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openSettings();
            }
        });
        if (bar.right != null) {
            bar.right.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.push(new M3SessionListPage(act));
                }
            });
        }
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(70)));

        scroll = new BounceScrollView(a);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(18), Ui.dp(4), Ui.dp(18), Ui.dp(116));
        scroll.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        slp.topMargin = Ui.dp(70);
        addView(scroll, slp);

        buildContent();

        nav = new M3NavBar(a);
        nav.selected(0);
        ArrayList<Session> all = SessionStore.list(a);
        nav.badge(all.size() == 0 ? "" : (all.size() > 9 ? "9+" : String.valueOf(all.size())));
        nav.listener(new M3NavBar.Listener() {
            public void onSelect(int index) {
                if (index == 1) act.push(new M3SessionListPage(act));
                else if (index == 2) act.push(new M3KnowledgePage(act));
                else if (index == 3) act.push(new M3FavoritePage(act));
                else if (index == 4) moreSheet();
                else if (index == 0) scroll.scrollTo(0, 0);
            }
        });
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(74), Gravity.BOTTOM);
        nlp.leftMargin = Ui.dp(12);
        nlp.rightMargin = Ui.dp(12);
        nlp.bottomMargin = Ui.dp(12);
        addView(nav, nlp);
    }

    @Override
    public void onPageShown() {
        buildContent();
        ArrayList<Session> all = SessionStore.list(act);
        nav.badge(all.size() == 0 ? "" : (all.size() > 9 ? "9+" : String.valueOf(all.size())));
        M3.reveal(content);
    }

    private void buildContent() {
        content.removeAllViews();

        Label hi = new Label(act, com.chatyue.app.util.TimeUtil.greeting(), 30f, M3.ON_SURFACE, true);
        content.addView(hi, lp(4, 14, 4, 0));
        Label sub = new Label(act, "今天想聊点什么？我都在。", 13f, M3.ON_SURFACE_2, false);
        content.addView(sub, lp(4, 5, 4, 0));

        content.addView(heroCard(), lp(0, 18, 0, 0));
        content.addView(quickRow(), lp(0, 12, 0, 0));

        content.addView(sectionHead("最近对话", "全部", new Runnable() {
            public void run() {
                act.push(new M3SessionListPage(act));
            }
        }), lp(4, 24, 4, 10));
        content.addView(recentCard(), lp(0, 0, 0, 0));

        content.addView(sectionHead("资料库", "", null), lp(4, 24, 4, 10));
        content.addView(knowledgeCard(), lp(0, 0, 0, 0));

        com.chatyue.app.ui.md.MdText tip = new com.chatyue.app.ui.md.MdText(act,
                "所有内容只保存在这台设备。长按消息可以复制、收藏或重新生成。",
                Ui.textPaint(M3.ON_SURFACE_3, 11.4f, false), 0, 0, 3f);
        content.addView(tip, lp(6, 18, 6, 0));
    }

    private View heroCard() {
        M3Card card = new M3Card(act, M3.PRIMARY_CONTAINER, 0);
        card.setRadius(28);
        card.pad(22, 20, 22, 20);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout tag = new LinearLayout(act);
        tag.setOrientation(LinearLayout.HORIZONTAL);
        tag.setGravity(Gravity.CENTER_VERTICAL);
        IconButtonMini badge = new IconButtonMini(act, Icons.SPARKLE);
        tag.addView(badge, new LinearLayout.LayoutParams(Ui.dp(22), Ui.dp(22)));
        Label tl = new Label(act, "  新的对话", 11.2f, M3.ON_PRIMARY_CONTAINER, true);
        tag.addView(tl, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        col.addView(tag, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Label title = new Label(act, "把想法说给我听", 22f, M3.ON_PRIMARY_CONTAINER, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(12);
        col.addView(title, tlp);

        Label desc = new Label(act, "文字、图片、代码、长文整理，都可以直接发过来。", 12.4f,
                Ui.alpha(M3.ON_PRIMARY_CONTAINER, 190), false);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = Ui.dp(7);
        col.addView(desc, dlp);

        M3Button start = new M3Button(act, "开始新对话", M3Button.FILLED).icon(Icons.PLUS);
        start.height(44);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openNewChat();
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(44));
        blp.topMargin = Ui.dp(16);
        col.addView(start, blp);

        card.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private View quickRow() {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addQuick(row, Icons.CLOCK, "定时任务", "拆成可执行的计划", "定时任务",
                "帮我把这件事拆成可执行的提醒计划：", 0);
        addQuick(row, Icons.EDIT, "快速记录", "整理成清晰条目", "快速记录",
                "帮我整理并归纳这段记录：", 1);
        addQuick(row, Icons.TRANSLATE, "快速翻译", "保留语气与节奏", "快速翻译",
                "把下面内容翻译成地道的英文，保留语气：\n", 2);
        return row;
    }

    private void addQuick(LinearLayout row, int icon, String title, String desc,
                          final String sessionTitle, final String preset, int index) {
        M3Card card = new M3Card(act);
        card.setRadius(22);
        card.pad(12, 13, 10, 12);
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        com.chatyue.app.ui.widget.IconButton b = new com.chatyue.app.ui.widget.IconButton(
                act, icon, M3.PRIMARY_CONTAINER, M3.PRIMARY_DEEP, 36, 17);
        col.addView(b, new LinearLayout.LayoutParams(Ui.dp(36), Ui.dp(36)));
        Label t = new Label(act, title, 11.8f, M3.ON_SURFACE, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(9);
        col.addView(t, tlp);
        Label d = new Label(act, desc, 9.8f, M3.ON_SURFACE_3, false);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = Ui.dp(3);
        col.addView(d, dlp);
        card.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openNewChatWithDraft(sessionTitle, preset);
            }
        });
        b.setClickable(false);
        b.setFocusable(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(112), 1f);
        if (index > 0) lp.leftMargin = Ui.dp(9);
        row.addView(card, lp);
    }

    private View sectionHead(String title, String action, final Runnable run) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Label t = new Label(act, title, 16.2f, M3.ON_SURFACE, true);
        row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (action != null && action.length() > 0) {
            Label a = new Label(act, action, 12f, M3.PRIMARY, true);
            a.setPadding(Ui.dp(6), Ui.dp(6), 0, Ui.dp(6));
            a.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (run != null) run.run();
                }
            });
            row.addView(a, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return row;
    }

    private View recentCard() {
        M3Card card = new M3Card(act);
        card.setRadius(26);
        card.pad(0, 4, 0, 4);
        ArrayList<Session> all = SessionStore.list(act);
        if (all.isEmpty()) {
            com.chatyue.app.ui.md.MdText empty = new com.chatyue.app.ui.md.MdText(act,
                    "还没有对话。点上面的「开始新对话」就能开始。",
                    Ui.textPaint(M3.ON_SURFACE_3, 12.6f, false), 0, 0, 3f);
            empty.setPadding(Ui.dp(20), Ui.dp(18), Ui.dp(20), Ui.dp(18));
            card.addView(empty, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            return card;
        }
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        int n = Math.min(4, all.size());
        for (int i = 0; i < n; i++) {
            final Session s = all.get(i);
            M3Row row = new M3Row(act, false);
            row.icon(Icons.MESSAGE);
            row.title(s.title);
            row.desc(trim(s.preview(), 26));
            row.value(com.chatyue.app.util.TimeUtil.friendly(s.updatedAt));
            row.divider(i < n - 1);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Session full = SessionStore.load(act, s.id);
                    if (full != null) act.openChat(full);
                }
            });
            col.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        card.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private View knowledgeCard() {
        M3Card card = new M3Card(act);
        card.setRadius(26);
        card.pad(0, 4, 0, 4);
        M3Row row = new M3Row(act, false);
        row.icon(Icons.BOOK);
        row.title("知识库");
        row.desc("已收录 " + Knowledge.all(act).size() + " 条 · 对话时自动参考");
        row.chevron(true);
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new M3KnowledgePage(act));
            }
        });
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void moreSheet() {
        M3Sheet sheet = new M3Sheet(act).title("更多").subtitle("常用入口都放在这里");
        sheet.add(Icons.GEAR, "设置", new Runnable() {
            public void run() {
                act.openSettings();
            }
        });
        sheet.add(Icons.PALETTE, "外观与主题", new Runnable() {
            public void run() {
                act.push(new M3AppearancePage(act));
            }
        });
        sheet.add(Icons.BOOK, "知识库", new Runnable() {
            public void run() {
                act.push(new M3KnowledgePage(act));
            }
        });
        sheet.divider();
        sheet.add(Icons.INFO, "关于 ChatYUE", new Runnable() {
            public void run() {
                M3Dialog.alert(act.root().overlay, "ChatYUE 3.0",
                        "最低支持 Android 1.5 的原生对话应用。\n经典主题为 Material 3 视觉，"
                                + "默认主题为手绘奶油风。\n\n所有会话保存在本机，不经过第三方服务器。");
            }
        });
        sheet.add(Icons.LOCK, "隐私说明", new Runnable() {
            public void run() {
                M3Dialog.alert(act.root().overlay, "隐私",
                        "应用不收集账号、通讯录与位置信息。\n会话内容仅写入应用私有目录；"
                                + "只有在发起对话时，才会把当前消息发送到你配置的模型端点。");
            }
        });
        sheet.show(act.root().overlay);
    }

    private LinearLayout.LayoutParams lp(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = Ui.dp(left);
        p.rightMargin = Ui.dp(right);
        p.topMargin = Ui.dp(top);
        p.bottomMargin = Ui.dp(bottom);
        return p;
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        s = s.replace('\n', ' ');
        if (s.length() > max) return s.substring(0, max) + "…";
        return s;
    }

    /** 小号矢量图标，用于 hero 标签，不参与点击。 */
    private static final class IconButtonMini extends View {
        private final int icon;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        IconButtonMini(android.content.Context c, int id) {
            super(c);
            icon = id;
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            Icons.draw(canvas, icon, getWidth() / 2f, getHeight() / 2f,
                    Math.min(getWidth(), getHeight()), M3.ON_PRIMARY_CONTAINER, 1.6f);
        }
    }
}
