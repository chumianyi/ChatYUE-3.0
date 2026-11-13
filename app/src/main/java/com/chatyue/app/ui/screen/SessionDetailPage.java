package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.util.TimeUtil;

/** 会话详情：重命名、导出、清空、删除。 */
public class SessionDetailPage extends BasePage {

    private final Session session;

    public SessionDetailPage(MainActivity a, Session s) {
        super(a, "会话信息", s.title == null ? "未命名" : s.title);
        session = s;
        build();
    }

    private void build() {
        resetContent();
        int user = 0;
        int chars = 0;
        for (int i = 0; i < session.messages.size(); i++) {
            Message m = session.messages.get(i);
            if (m.isUser()) user++;
            chars += m.text == null ? 0 : m.text.length();
        }

        heading(session.title == null || session.title.length() == 0 ? "未命名会话" : session.title,
                "创建于 " + TimeUtil.friendly(session.createdAt));

        section("概览");
        CardList meta = card();
        meta.padding(16, 12);
        meta.add(info("消息", session.messages.size() + " 条（你 " + user + " 条）"));
        meta.add(info("字数", "约 " + chars + " 字"));
        meta.add(info("最近", TimeUtil.friendly(session.updatedAt)));
        meta.add(info("会话 ID", session.id));

        section("操作");
        row(Icons.EDIT, "重命名", "起一个好找的名字", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.input(act.root().overlay, "重命名会话", "新标题",
                        session.title == null ? "" : session.title, new Dialog.InputCallback() {
                            public void onInput(String text) {
                                if (text == null || text.trim().length() == 0) return;
                                session.title = text.trim();
                                SessionStore.save(act, session);
                                bar.setTitle(session.title);
                                bar.setSubtitle("已保存");
                                build();
                                act.toast("已重命名");
                            }
                        });
            }
        });
        row(Icons.COPY, "导出为文本", "复制全部对话到剪贴板", new View.OnClickListener() {
            public void onClick(View v) {
                String text = transcript();
                act.copyText(text);
            }
        });
        row(Icons.TRASH, "清空消息", "保留会话，只删内容", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.confirmDanger(act.root().overlay, "清空消息？", "这个会话的 " + session.messages.size()
                        + " 条消息会被删除，会话本身保留。", "清空", new Runnable() {
                    public void run() {
                        session.messages.clear();
                        SessionStore.save(act, session);
                        build();
                        act.toast("已清空");
                    }
                });
            }
        });
        row(Icons.TRASH, "删除会话", "不可恢复", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.confirmDanger(act.root().overlay, "删除会话？",
                        (session.title == null ? "这个会话" : session.title) + " 将被永久删除。", "删除", new Runnable() {
                            public void run() {
                                SessionStore.delete(act, session.id);
                                act.toast("已删除");
                                act.showHome();
                            }
                        });
            }
        }).danger(true);
    }

    private Label info(String k, String v) {
        Label l = new Label(act, k + "　　" + v, 12.4f, Palette.INK_2, false);
        l.setPadding(0, 5, 0, 5);
        return l;
    }

    private String transcript() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(session.title == null ? "未命名会话" : session.title).append("\n\n");
        for (int i = 0; i < session.messages.size(); i++) {
            Message m = session.messages.get(i);
            sb.append(m.isUser() ? "我：" : "ChatYUE：").append(m.text == null ? "" : m.text).append("\n\n");
        }
        sb.append("— 由 ChatYUE 导出");
        return sb.toString();
    }

    @Override
    public void onPageShown() {
        bar.setSubtitle(session.title == null ? "未命名" : session.title);
    }
}
