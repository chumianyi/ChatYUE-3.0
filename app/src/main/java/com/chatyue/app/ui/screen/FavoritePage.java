package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.EmptyState;
import com.chatyue.app.ui.widget.MenuRow;

import java.util.ArrayList;

/** 收藏：跨会话收集重要的回复。 */
public class FavoritePage extends BasePage {

    public FavoritePage(MainActivity a) {
        super(a, "收藏", "长按回复即可收藏");
        build();
    }

    private void build() {
        resetContent();
        ArrayList<Session> sessions = SessionStore.list(act);
        ArrayList<Session> hitSession = new ArrayList<Session>();
        ArrayList<Message> hitMsg = new ArrayList<Message>();

        for (int i = 0; i < sessions.size(); i++) {
            Session full = SessionStore.load(act, sessions.get(i).id);
            if (full == null) continue;
            for (int k = 0; k < full.messages.size(); k++) {
                Message m = full.messages.get(k);
                if (m.starred) {
                    hitSession.add(full);
                    hitMsg.add(m);
                }
            }
        }

        heading("收起来的答案", hitMsg.size() == 0
                ? "还没有收藏。长按任意一条回复，选择「收藏」。"
                : "共 " + hitMsg.size() + " 条。点击回到原会话。");

        if (hitMsg.isEmpty()) {
            content.addView(new EmptyState(act, Icons.STAR, "空空的收藏夹",
                    "长按一条回复 → 收藏，之后在这里就能快速找到。"), lp(0, 6, 0, 0));
            return;
        }

        CardList card = card();
        for (int i = hitMsg.size() - 1; i >= 0; i--) {
            final Session s = hitSession.get(i);
            final Message m = hitMsg.get(i);
            String text = m.text == null ? "" : m.text.replace('\n', ' ');
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(false, false);
            r.icon(Icons.STAR, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(text.length() > 34 ? text.substring(0, 34) + "…" : text);
            r.desc(s.title == null ? "未命名会话" : s.title);
            r.chevron(true);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.openChat(s);
                }
            });
            card.addRow(r);
        }
        note("再次长按可以取消收藏，条目会立即从这里消失。");
    }

    @Override
    public void onPageShown() {
        build();
    }
}
