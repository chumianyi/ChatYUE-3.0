package com.chatyue.app.ui.screen;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.EmptyState;
import com.chatyue.app.ui.widget.FieldBox;
import com.chatyue.app.ui.widget.MenuRow;

import java.util.ArrayList;

/**
 * 全局搜索：同时搜会话标题与消息正文。
 * 输入即搜（防抖 180ms），结果按「标题命中 → 正文命中」排序。
 */
public class SearchPage extends BasePage {

    private FieldBox input;
    private LinearLayout results;
    private String keyword = "";
    private Runnable pending;

    public SearchPage(MainActivity a) {
        super(a, "搜索", "会话标题与消息正文");
        build();
    }

    private void build() {
        resetContent();
        heading("找一段对话", "输入关键词，标题和正文都会搜。");

        input = new FieldBox(act, null, "搜索会话或消息内容", "", 1);
        content.addView(input, lp(16, 6, 16, 12));
        input.edit.setSingleLine(true);

        results = new LinearLayout(act);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        input.edit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                keyword = s.toString().trim();
                if (pending != null) results.removeCallbacks(pending);
                pending = new Runnable() {
                    public void run() {
                        search();
                    }
                };
                results.postDelayed(pending, 180);
            }
        });

        showHint();
    }

    private void showHint() {
        results.removeAllViews();
        results.addView(new EmptyState(act, Icons.SEARCH, "搜点什么",
                "例如：报错日志、旅行计划，或者你记得的某句话。"), lp(0, 4, 0, 0));
    }

    /** 设置项索引：让「设置 - 搜索」也能真的搜到页面。 */
    private static final String[][] SETTINGS = {
            {"用户画像", "profile"}, {"助手与角色", "assistant"}, {"Agent 能力", "agent"},
            {"助手资源", "assets"}, {"聊天设置", "chat"}, {"外观与主题", "look"},
            {"回答语言", "lang"}, {"模型参数", "params"}, {"全部会话", "sessions"},
            {"数据与存储", "data"}, {"关于 ChatYUE", "about"}, {"帮助与反馈", "help"},
            {"用户协议", "terms"}, {"隐私政策", "privacy"}
    };

    private void search() {
        results.removeAllViews();
        if (keyword.length() == 0) {
            showHint();
            return;
        }
        String kw = keyword.toLowerCase();
        ArrayList<Session> sessions = SessionStore.list(act);
        int shown = 0;

        // 第零轮：设置项
        for (int i = 0; i < SETTINGS.length; i++) {
            if (SETTINGS[i][0].toLowerCase().indexOf(kw) >= 0) {
                addSetting(SETTINGS[i][0], SETTINGS[i][1]);
                shown++;
            }
        }
        if (shown > 0) results.addView(caption("设置"), lp(20, 10, 20, 6));

        // 第一轮：标题命中
        for (int i = 0; i < sessions.size() && shown < 12; i++) {
            final Session meta = sessions.get(i);
            String title = meta.title == null ? "" : meta.title;
            if (title.toLowerCase().indexOf(kw) >= 0) {
                addResult(meta, "标题命中", title, true);
                shown++;
            }
        }

        // 第二轮：正文命中（每个会话最多一条）
        for (int i = 0; i < sessions.size() && shown < 24; i++) {
            final Session meta = sessions.get(i);
            Session full = SessionStore.load(act, meta.id);
            if (full == null) continue;
            for (int k = full.messages.size() - 1; k >= 0; k--) {
                Message m = full.messages.get(k);
                String text = m.text == null ? "" : m.text;
                int at = text.toLowerCase().indexOf(kw);
                if (at < 0) continue;
                int from = Math.max(0, at - 18);
                int to = Math.min(text.length(), at + 42);
                String snip = (from > 0 ? "…" : "") + text.substring(from, to).replace('\n', ' ')
                        + (to < text.length() ? "…" : "");
                addResult(meta, m.isUser() ? "你说" : "回复", snip, false);
                shown++;
                break;
            }
        }

        if (shown == 0) {
            results.addView(new EmptyState(act, Icons.SEARCH, "没有找到",
                    "换个更短的关键词试试，比如只输入两个字。"), lp(0, 4, 0, 0));
        } else {
            results.addView(caption("找到 " + shown + " 条"), lp(20, 12, 20, 8));
        }
    }

    private void addSetting(String title, final String id) {
        LinearLayout wrap = new LinearLayout(act);
        wrap.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
        CardList card = new CardList(act);
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(true, true);
        r.icon(Icons.GEAR, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(title);
        r.desc("设置项");
        r.chevron(true);
        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openSetting(id);
            }
        });
        card.addRow(r);
        wrap.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(8);
        results.addView(wrap, p);
    }

    private void openSetting(String id) {
        if ("profile".equals(id)) act.push(new ProfilePage(act));
        else if ("assistant".equals(id)) act.push(new AssistantListPage(act));
        else if ("agent".equals(id)) act.push(new AgentPage(act));
        else if ("assets".equals(id)) act.push(new AssetsPage(act));
        else if ("chat".equals(id)) act.push(new ChatSettingsPage(act));
        else if ("look".equals(id)) act.push(new AppearancePage(act));
        else if ("lang".equals(id)) act.push(new LanguagePage(act));
        else if ("params".equals(id)) act.push(new ModelParamsPage(act));
        else if ("sessions".equals(id)) act.push(new SessionListPage(act));
        else if ("data".equals(id)) act.push(new DataPage(act));
        else if ("about".equals(id)) act.push(new AboutPage(act));
        else if ("help".equals(id)) act.push(new HelpPage(act));
        else if ("terms".equals(id)) act.push(new TermsPage(act));
        else if ("privacy".equals(id)) act.push(new PrivacyPage(act));
    }

    private View caption(String t) {
        com.chatyue.app.ui.widget.Label l =
                new com.chatyue.app.ui.widget.Label(act, t, 11.8f, Palette.INK_3, false);
        return l;
    }

    private void addResult(final Session meta, String badge, String text, boolean titleHit) {
        LinearLayout wrap = new LinearLayout(act);
        wrap.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
        CardList card = new CardList(act);
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(true, true);
        r.icon(titleHit ? Icons.LIST : Icons.DOT, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title((meta.title == null || meta.title.length() == 0 ? "未命名会话" : meta.title));
        r.desc(badge + " · " + text);
        r.chevron(true);
        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Session full = SessionStore.load(act, meta.id);
                if (full != null) act.openChat(full);
            }
        });
        card.addRow(r);
        wrap.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(8);
        results.addView(wrap, lp);
    }
}
