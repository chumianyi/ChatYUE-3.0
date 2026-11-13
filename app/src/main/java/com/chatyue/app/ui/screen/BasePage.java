package com.chatyue.app.ui.screen;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.md.MarkdownView;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.EmptyState;
import com.chatyue.app.ui.widget.FieldBox;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.SectionLabel;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.Stepper;
import com.chatyue.app.ui.widget.Toggle;
import com.chatyue.app.ui.widget.TopBar;

/**
 * 二级页面基类：顶栏 + 可回弹滚动区 + 一套「分组标题 / 卡片 / 行 / 说明」的排版原语。
 *
 * 所有二级页面都只在这里搭骨架，具体内容由子类在构造时声明，
 * 这样 30 多个页面的视觉节奏完全一致，也避免了每个页面各写一遍 LinearLayout 参数。
 */
public abstract class BasePage extends Page {

    protected final TopBar bar;
    protected final BounceScrollView scroll;
    protected final LinearLayout content;

    private CardList card;

    public BasePage(MainActivity a, String title, String subtitle) {
        super(a);
        bar = new TopBar(a, title, subtitle == null ? "" : subtitle, 0);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(66)));

        scroll = new BounceScrollView(a);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, Ui.dp(2), 0, Ui.dp(34));
        scroll.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        lp.topMargin = Ui.dp(66);
        addView(scroll, lp);
    }

    @Override
    public boolean onBack() {
        act.pop();
        return true;
    }

    // ------------------------------------------------------------ 排版原语

    protected LinearLayout.LayoutParams lp(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = Ui.dp(left);
        p.rightMargin = Ui.dp(right);
        p.topMargin = Ui.dp(top);
        p.bottomMargin = Ui.dp(bottom);
        return p;
    }

    /** 顶部大标题（用于概览型页面）。 */
    protected void heading(String title, String desc) {
        Label t = new Label(act, title, 21f, Palette.INK, true);
        content.addView(t, lp(20, 8, 20, 0));
        if (desc != null && desc.length() > 0) {
            Label d = new Label(act, desc, 12.6f, Palette.INK_3, false);
            content.addView(d, lp(20, 7, 22, 2));
        }
    }

    protected void section(String title) {
        card = null;
        content.addView(new SectionLabel(act, title), lp(0, 20, 0, 8));
    }

    /** 页面自刷新：清空内容后由子类重新声明一遍（用于列表增删）。 */
    protected void resetContent() {
        content.removeAllViews();
        card = null;
    }

    protected void space(int dp) {
        View v = new View(act);
        content.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(dp)));
    }

    protected CardList card() {
        if (card == null) {
            card = new CardList(act);
            content.addView(card, lp(16, 0, 16, 0));
        }
        return card;
    }

    protected CardList newCard(int topMargin) {
        card = new CardList(act);
        content.addView(card, lp(16, topMargin, 16, 0));
        return card;
    }

    protected MenuRow row(int icon, String title, String desc, View.OnClickListener l) {
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(icon, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(title);
        if (desc != null && desc.length() > 0) r.desc(desc);
        r.chevron(l != null);
        if (l != null) r.setOnClickListener(l);
        card().addRow(r);
        return r;
    }

    protected MenuRow rowWithValue(int icon, String title, String value, View.OnClickListener l) {
        MenuRow r = row(icon, title, null, l);
        r.value(value);
        return r;
    }

    /** 开关行：整行可点，点哪都能切换。 */
    protected Toggle toggleRow(int icon, String title, String desc, boolean checked,
                               final Toggle.Listener listener) {
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(icon, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(title);
        if (desc != null && desc.length() > 0) r.desc(desc);
        r.chevron(false);
        final Toggle t = new Toggle(act);
        t.setChecked(checked);
        t.listener(listener);
        r.trailing(t);
        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t.setChecked(!t.isChecked(), true);
                if (listener != null) listener.onToggle(t.isChecked());
            }
        });
        card().addRow(r);
        return t;
    }

    protected Stepper stepperRow(int icon, String title, String desc, int value,
                                 int min, int max, int step, String suffix,
                                 Stepper.Listener listener) {
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(icon, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(title);
        if (desc != null && desc.length() > 0) r.desc(desc);
        r.chevron(false);
        Stepper s = new Stepper(act);
        s.range(min, max, step).suffix(suffix).listener(listener);
        s.setValue(value);
        r.trailing(s);
        card().addRow(r);
        return s;
    }

    /** 卡片下方的灰色说明。 */
    protected void note(String text) {
        Label l = new Label(act, text, 11.6f, Palette.INK_3, false);
        l.setPadding(Ui.dp(2), 0, Ui.dp(2), 0);
        content.addView(l, lp(22, 9, 22, 0));
    }

    /** Markdown 正文（协议、帮助等长文）。 */
    protected void markdown(String md) {
        MarkdownView v = new MarkdownView(act, Prefs.fontScale(act));
        v.setMarkdown(md);
        content.addView(v, lp(20, 6, 20, 0));
    }

    /** 卡片式输入框，返回包装对象以便读取内容。 */
    protected FieldBox field(String label, String hint, String value, int lines) {
        FieldBox box = new FieldBox(act, label, hint, value, lines);
        content.addView(box, lp(16, 0, 16, 10));
        return box;
    }

    protected SoftButton primaryButton(String text, View.OnClickListener l) {
        SoftButton b = new SoftButton(act, text, 14.6f, 0xFFFFFFFF, Palette.PINK);
        b.radius(17);
        b.bold(true);
        if (l != null) b.setOnClickListener(l);
        LinearLayout.LayoutParams p = lp(16, 18, 16, 0);
        p.height = Ui.dp(50);
        content.addView(b, p);
        return b;
    }

    protected SoftButton ghostButton(String text, View.OnClickListener l) {
        SoftButton b = new SoftButton(act, text, 13.6f, Palette.INK_2, Palette.CARD);
        b.radius(17);
        if (l != null) b.setOnClickListener(l);
        LinearLayout.LayoutParams p = lp(16, 10, 16, 0);
        p.height = Ui.dp(46);
        content.addView(b, p);
        return b;
    }

    protected void empty(int icon, String title, String desc) {
        card = null;
        content.addView(new EmptyState(act, icon, title, desc), lp(0, 6, 0, 0));
    }
}
