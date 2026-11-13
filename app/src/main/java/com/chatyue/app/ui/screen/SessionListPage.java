package com.chatyue.app.ui.screen;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.SessionRow;
import com.chatyue.app.ui.widget.TopBar;

import java.util.ArrayList;

/** 会话列表页：ListView + 自定义 Adapter，支持切换 / 新建 / 重命名 / 删除。 */
public class SessionListPage extends Page {

    private final ArrayList<Session> data = new ArrayList<Session>();
    private ListView list;
    private Adapter adapter;
    private Label empty;
    private TopBar bar;

    public SessionListPage(MainActivity a) {
        super(a);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        bar = new TopBar(a, "会话", "", Icons.PLUS);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        if (bar.right != null) {
            bar.right.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.openNewChat();
                }
            });
        }
        root.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(66)));

        list = new ListView(a);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(new android.graphics.drawable.ColorDrawable(0x00000000));
        list.setCacheColorHint(0);
        list.setVerticalScrollBarEnabled(false);
        list.setPadding(0, Ui.dp(4), 0, Ui.dp(20));
        list.setClipToPadding(false);
        adapter = new Adapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        empty = new Label(a, "还没有会话，点右上角 + 新建", 13f, Palette.INK_3, false);
        empty.setAlign(Label.CENTER);
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        addView(empty, elp);

        reload();
    }

    @Override
    public void onPageShown() {
        reload();
    }

    private void reload() {
        data.clear();
        data.addAll(SessionStore.list(act));
        if (bar != null) bar.setSubtitle(data.size() + " 个会话");
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void open(Session meta) {
        Session full = SessionStore.load(act, meta.id);
        if (full == null) {
            act.toast("会话已损坏");
            return;
        }
        act.openChat(full);
    }

    private void actions(final Session s) {
        Sheet sheet = new Sheet(act).title(s.title).subtitle(s.preview());
        sheet.add(Icons.EDIT, "打开", new Runnable() {
            public void run() {
                open(s);
            }
        });
        sheet.add(Icons.EDIT, "重命名", new Runnable() {
            public void run() {
                Dialog.input(act.root().overlay, "重命名", "会话标题", s.title,
                        new Dialog.InputCallback() {
                            public void onInput(String text) {
                                if (text.length() == 0) return;
                                Session full = SessionStore.load(act, s.id);
                                if (full == null) return;
                                full.title = text;
                                SessionStore.save(act, full);
                                reload();
                            }
                        });
            }
        });
        sheet.addDanger(Icons.TRASH, "删除会话", new Runnable() {
            public void run() {
                Dialog.confirmDanger(act.root().overlay, "删除会话？", s.title + " 将被永久删除。", "删除",
                        new Runnable() {
                            public void run() {
                                SessionStore.delete(act, s.id);
                                reload();
                                act.toast("已删除");
                            }
                        });
            }
        });
        sheet.show(act.root().overlay);
    }

    private class Adapter extends BaseAdapter {
        public int getCount() {
            return data.size();
        }

        public Object getItem(int position) {
            return data.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout wrap;
            SessionRow row;
            if (convertView instanceof FrameLayout && convertView.getTag() instanceof SessionRow) {
                wrap = (FrameLayout) convertView;
                row = (SessionRow) convertView.getTag();
            } else {
                wrap = new FrameLayout(act);
                wrap.setPadding(Ui.dp(16), Ui.dp(5), Ui.dp(16), Ui.dp(5));
                row = new SessionRow(act, data.get(position), false, false);
                wrap.addView(row, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
                wrap.setTag(row);
            }
            final Session s = data.get(position);
            row.setSession(s, false, position == data.size() - 1);
            row.setListener(new SessionRow.Listener() {
                public void onClick(Session session) {
                    open(session);
                }

                public void onDelete(Session session) {
                    actions(session);
                }

                public void onLongClick(Session session) {
                    actions(session);
                }
            });
            return wrap;
        }
    }
}
