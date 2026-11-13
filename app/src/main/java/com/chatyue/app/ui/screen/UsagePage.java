package com.chatyue.app.ui.screen;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;

import java.util.ArrayList;

/** 用量统计：按本机数据估算，不假装是服务商的计费口径。 */
public class UsagePage extends BasePage {

    public UsagePage(MainActivity a) {
        super(a, "用量统计", "估算值");
        build();
    }

    private void build() {
        resetContent();
        ArrayList<Session> metas = SessionStore.list(act);
        int msgCount = 0;
        int userCount = 0;
        int chars = 0;
        ArrayList<String> titles = new ArrayList<String>();
        ArrayList<Integer> counts = new ArrayList<Integer>();

        for (int i = 0; i < metas.size(); i++) {
            Session s = SessionStore.load(act, metas.get(i).id);
            if (s == null) continue;
            int c = 0;
            for (int k = 0; k < s.messages.size(); k++) {
                Message m = s.messages.get(k);
                msgCount++;
                if (m.isUser()) userCount++;
                chars += m.text == null ? 0 : m.text.length();
                c++;
            }
            titles.add(s.title == null ? "未命名" : s.title);
            counts.add(Integer.valueOf(c));
        }

        // 中文约 1.6 字/token，英文约 4 字符/token，这里按 1.6 粗估
        int tokens = (int) (chars / 1.6f);

        heading("大概用了多少", "数据来自本机会话文件，只是估算，不等于服务商账单。");

        CardList card = card();
        card.padding(16, 14);
        card.add(big(String.valueOf(metas.size()), "个会话"));
        card.add(big(String.valueOf(msgCount), "条消息（你 " + userCount + " 条）"));
        card.add(big("约 " + tokens, "tokens 输入总量"));
        card.add(big(String.valueOf(chars), "字"));

        section("会话排行");
        if (titles.isEmpty()) {
            note("还没有会话。");
            return;
        }
        CardList rank = newCard(0);
        int limit = Math.min(6, titles.size());
        for (int i = 0; i < limit; i++) {
            int best = -1;
            int bestCount = -1;
            for (int k = 0; k < titles.size(); k++) {
                int c = counts.get(k).intValue();
                if (c > bestCount) {
                    bestCount = c;
                    best = k;
                }
            }
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(false, false);
            r.icon(i == 0 ? com.chatyue.app.core.Icons.STAR : com.chatyue.app.core.Icons.DOT,
                    Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(titles.get(best));
            r.value(String.valueOf(bestCount) + " 条");
            r.chevron(false);
            rank.addRow(r);
            titles.remove(best);
            counts.remove(best);
        }

        note("估算公式：字符数 ÷ 1.6。中英文混排误差较大，仅供横向比较。");
    }

    private Label big(String value, String unit) {
        Label l = new Label(act, value + "  " + unit, 15.2f, Palette.INK, false);
        l.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
        return l;
    }
}
