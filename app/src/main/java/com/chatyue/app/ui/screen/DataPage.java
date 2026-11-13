package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;

import java.io.File;
import java.util.ArrayList;

/** 数据与存储：看得见的文件、可执行的清理。 */
public class DataPage extends BasePage {

    public DataPage(MainActivity a) {
        super(a, "数据与存储", "全部在本机");
        build();
    }

    private void build() {
        resetContent();
        ArrayList<Session> sessions = SessionStore.list(act);
        int messages = 0;
        for (int i = 0; i < sessions.size(); i++) {
            Session s = SessionStore.load(act, sessions.get(i).id);
            if (s != null) messages += s.messages.size();
        }
        long sessionBytes = dirSize(SessionStore.dir(act));
        long imageBytes = dirSize(SessionStore.imageDir(act));

        heading("数据都在这里", "没有账号系统，也没有云端同步。卸载应用即全部删除。");

        section("占用");
        CardList card = card();
        card.padding(16, 12);
        card.add(line("会话文件", sessions.size() + " 个 · " + kb(sessionBytes)));
        card.add(line("消息", messages + " 条"));
        card.add(line("图片缓存", kb(imageBytes)));
        card.add(line("位置", SessionStore.dir(act).getAbsolutePath()));

        section("清理");
        row(Icons.TRASH, "清理图片缓存", "只删聊天图片，不动文字", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.confirm(act.root().overlay, "清理图片缓存？", "已发送过的图片会从本机删除，历史消息里的图片将无法显示。",
                        "清理", new Runnable() {
                            public void run() {
                                deleteAll(SessionStore.imageDir(act));
                                act.toast("图片缓存已清理");
                                build();
                            }
                        });
            }
        });
        row(Icons.TRASH, "删除全部会话", "不可恢复", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.confirmDanger(act.root().overlay, "删除全部会话？",
                        "所有会话与消息都会被永久删除，这个操作不可撤销。", "全部删除", new Runnable() {
                            public void run() {
                                ArrayList<Session> all = SessionStore.list(act);
                                for (int i = 0; i < all.size(); i++) {
                                    SessionStore.delete(act, all.get(i).id);
                                }
                                act.toast("已删除全部会话");
                                act.showHome();
                            }
                        });
            }
        }).danger(true);

        section("其它");
        row(Icons.REFRESH, "重新播放新手引导", "再看一遍开场的 9 步", new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.setOnboarded(act, false);
                act.showOnboarding();
            }
        });
        row(Icons.INFO, "关于与协议", "版本、用户协议、隐私政策", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new AboutPage(act));
            }
        });

        note("会话文件是明文 JSON，位于应用私有目录，其它应用无法读取。");
    }

    private Label line(String k, String v) {
        Label l = new Label(act, k + "　　" + v, 12.4f, Palette.INK_2, false);
        l.setPadding(0, 5, 0, 5);
        return l;
    }

    private String kb(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / 1024 / 1024) + " MB";
    }

    private long dirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        long total = 0;
        for (int i = 0; i < files.length; i++) {
            total += files[i].isDirectory() ? dirSize(files[i]) : files[i].length();
        }
        return total;
    }

    private void deleteAll(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) deleteAll(files[i]);
            else files[i].delete();
        }
    }
}
