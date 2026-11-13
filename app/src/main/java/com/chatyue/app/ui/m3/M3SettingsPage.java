package com.chatyue.app.ui.m3;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Theme;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Assistants;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Provider;
import com.chatyue.app.data.Providers;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.Avatar;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.Label;

import java.util.ArrayList;

/** 经典主题设置：大标题、账号卡、搜索、分组列表和 M3 底部面板。 */
public class M3SettingsPage extends Page {

    private LinearLayout content;
    private LinearLayout body;
    private M3SearchField search;
    private String filter = "";

    public M3SettingsPage(MainActivity a) {
        super(a);
        setBackgroundColor(M3.BG);

        M3TopBar bar = new M3TopBar(a, "设置", "按你的习惯来", Icons.BACK, Icons.SEARCH);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        bar.right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new M3SessionListPage(act));
            }
        });
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(70)));

        BounceScrollView scroll = new BounceScrollView(a);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(18), Ui.dp(2), Ui.dp(18), Ui.dp(30));
        scroll.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        slp.topMargin = Ui.dp(70);
        addView(scroll, slp);

        search = new M3SearchField(a, "搜索设置");
        search.listener(new M3SearchField.Listener() {
            public void onText(String text) {
                filter = text == null ? "" : text.trim().toLowerCase();
                buildRows();
            }
        });
        content.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(50)));

        body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        content.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        buildRows();
        M3.reveal(body);
    }

    @Override
    public void onPageShown() {
        buildRows();
    }

    /** 搜索框固定在顶部，每次筛选只重建下面这块，输入焦点不会丢。 */
    private void buildRows() {
        body.removeAllViews();
        if (filter.length() == 0) body.addView(profileCard(), lp(0, 14, 0, 0));

        M3Card pref = new M3Card(act);
        pref.setRadius(26);
        pref.pad(0, 3, 0, 3);
        buildPrefRows(pref);
        if (rowCount > 0) {
            body.addView(section("偏好", true));
            body.addView(pref, lp(0, 0, 0, 0));
        }

        M3Card about = new M3Card(act);
        about.setRadius(26);
        about.pad(0, 3, 0, 3);
        buildAboutRows(about);
        if (rowCount2 > 0) {
            body.addView(section("关于", true));
            body.addView(about, lp(0, 0, 0, 0));
        }

        com.chatyue.app.ui.md.MdText note = new com.chatyue.app.ui.md.MdText(act,
                "经典主题使用独立的 Material 3 页面与组件，和默认手绘主题不是同一套布局。",
                Ui.textPaint(M3.ON_SURFACE_3, 10.8f, false), 0, 0, 3f);
        body.addView(note, lp(6, 18, 6, 0));
    }

    private M3Card profileCard() {
        M3Card card = new M3Card(act, M3.PRIMARY_CONTAINER, 0);
        card.setRadius(28);
        card.pad(18, 18, 18, 18);
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Avatar av = new Avatar(act);
        av.setLetter(firstLetter(Prefs.nickname(act)));
        av.setColors(M3.SURFACE, M3.PRIMARY_DEEP);
        row.addView(av, new LinearLayout.LayoutParams(Ui.dp(56), Ui.dp(56)));

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        Label name = new Label(act, Prefs.nickname(act), 20f, M3.ON_PRIMARY_CONTAINER, true);
        col.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Label sub = new Label(act, "本地优先 · 不需要登录账号", 10.8f,
                Ui.alpha(M3.ON_PRIMARY_CONTAINER, 190), false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(4);
        col.addView(sub, slp);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(14);
        row.addView(col, clp);

        M3Button edit = new M3Button(act, "改名", M3Button.FILLED);
        edit.height(38);
        edit.padH(16);
        edit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                M3Dialog.input(act.root().overlay, "你的称呼", "例如：初眠", Prefs.nickname(act),
                        new M3Dialog.InputCallback() {
                            public void onInput(String text) {
                                if (text.length() == 0) return;
                                Prefs.setString(act, Prefs.KEY_NICKNAME, text);
                                buildRows();
                                act.toast("已更新称呼");
                            }
                        });
            }
        });
        row.addView(edit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(38)));
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private View section(String title, boolean show) {
        Label l = new Label(act, title, 13.2f, M3.ON_SURFACE_2, true);
        l.setPadding(Ui.dp(6), Ui.dp(18), 0, Ui.dp(8));
        return l;
    }

    private int rowCount;
    private int rowCount2;

    private void addRow(M3Card card, int icon, String title, String desc, String value,
                        final Runnable action) {
        if (filter.length() > 0) {
            String hay = (title + " " + desc).toLowerCase();
            if (!hay.contains(filter)) return;
        }
        M3Row row = new M3Row(act, false);
        row.icon(icon);
        row.title(title).desc(desc).value(value).chevron(true);
        row.divider(rowCount > 0);
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                action.run();
            }
        });
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        rowCount++;
    }

    private void buildPrefRows(M3Card card) {
        rowCount = 0;
        addRow(card, Icons.PALETTE, "外观", "主题 · 字号 · 实时预览",
                Theme.name(Prefs.theme(act)), new Runnable() {
                    public void run() {
                        act.push(new M3AppearancePage(act));
                    }
                });
        addRow(card, Icons.EDIT, "对话与输入", "记忆 · 回车发送 · 自动滚动",
                Prefs.getBool(act, Prefs.KEY_MEMORY, true) ? "记忆已开" : "记忆已关",
                new Runnable() {
                    public void run() {
                        chatSheet();
                    }
                });
        addRow(card, Icons.ROBOT, "模型与提供方", "默认模型 · 端点",
                Providers.current(act).name, new Runnable() {
                    public void run() {
                        modelSheet();
                    }
                });
        addRow(card, Icons.SPARKLE, "助手人设", "默认助手 · 全局提示词",
                Assistants.current(act).name, new Runnable() {
                    public void run() {
                        assistantSheet();
                    }
                });
        addRow(card, Icons.BOOK, "知识库", "对话时自动参考", Knowledge.all(act).size() + " 条",
                new Runnable() {
                    public void run() {
                        act.push(new M3KnowledgePage(act));
                    }
                });
        addRow(card, Icons.DOC, "数据与缓存", "会话 · 图片 · 清空", SessionStore.list(act).size() + " 段",
                new Runnable() {
                    public void run() {
                        dataSheet();
                    }
                });
    }

    private void buildAboutRows(M3Card card) {
        rowCount2 = 0;
        addRow2(card, Icons.SPARKLE, "实验室", "反思 · 多轮 Agent · 全部文件",
                Prefs.agentEnabled(act) ? "已开启" : "默认关闭", new Runnable() {
                    public void run() {
                        act.push(new com.chatyue.app.ui.screen.ExperimentalPage(act));
                    }
                });
        addRow2(card, Icons.INFO, "关于 ChatYUE", "版本 · 原项目 · 致谢", "3.0", new Runnable() {
            public void run() {
                act.push(new com.chatyue.app.ui.screen.AboutPage(act));
            }
        });
        addRow2(card, Icons.WAIT, "帮助与反馈", "常见问题 · 使用技巧", "", new Runnable() {
            public void run() {
                helpSheet();
            }
        });
        addRow2(card, Icons.LOCK, "隐私", "本地优先 · 无账号", "", new Runnable() {
            public void run() {
                privacySheet();
            }
        });
    }

    private void addRow2(M3Card card, int icon, String title, String desc, String value,
                         final Runnable action) {
        if (filter.length() > 0) {
            String hay = (title + " " + desc).toLowerCase();
            if (!hay.contains(filter)) return;
        }
        M3Row row = new M3Row(act, false);
        row.icon(icon);
        row.title(title).desc(desc).value(value).chevron(true);
        row.divider(rowCount2 > 0);
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                action.run();
            }
        });
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        rowCount2++;
    }

    private void chatSheet() {
        M3Sheet sheet = new M3Sheet(act).title("对话与输入").subtitle("即时生效，下一轮对话就会用上");
        sheet.addSwitch(Icons.EDIT, "参考记忆", "把知识库与画像带进上下文",
                Prefs.getBool(act, Prefs.KEY_MEMORY, true), new M3Switch.Listener() {
                    public void onChanged(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_MEMORY, checked);
                        buildRows();
                        act.toast(checked ? "已开启记忆" : "已关闭记忆");
                    }
                });
        sheet.addSwitch(Icons.SEND, "回车发送", "关闭后回车只换行",
                Prefs.getBool(act, Prefs.KEY_ENTER_SEND, true), new M3Switch.Listener() {
                    public void onChanged(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_ENTER_SEND, checked);
                        buildRows();
                    }
                });
        sheet.addSwitch(Icons.ARROW_UP, "自动滚动", "流式输出时跟随最新内容",
                Prefs.getBool(act, Prefs.KEY_AUTOSCROLL, true), new M3Switch.Listener() {
                    public void onChanged(boolean checked) {
                        Prefs.setBool(act, Prefs.KEY_AUTOSCROLL, checked);
                        buildRows();
                    }
                });
        sheet.divider();
        sheet.addDesc(Icons.PALETTE, "字号", "当前 " + Prefs.fontPercent(act) + "% · 点按循环切换",
                new Runnable() {
                    public void run() {
                        Prefs.cycleFont(act);
                        buildRows();
                        act.toast("字号：" + Prefs.fontLabel(act) + "（重开对话生效）");
                    }
                });
        sheet.show(act.root().overlay);
    }

    private void modelSheet() {
        M3Sheet sheet = new M3Sheet(act).title("默认模型").subtitle("当前：" + Providers.current(act).name);
        ArrayList<Provider> all = Providers.all(act);
        String def = Providers.currentId(act);
        for (int i = 0; i < all.size(); i++) {
            final Provider p = all.get(i);
            sheet.addChecked(p.isBuiltin() ? Icons.SPARKLE : Icons.ROBOT,
                    p.name + " · " + p.model, p.id.equals(def), new Runnable() {
                        public void run() {
                            Providers.setDefault(act, p.id);
                            buildRows();
                            act.toast("默认模型：" + p.name);
                        }
                    });
        }
        sheet.divider();
        sheet.add(Icons.PLUS, "添加第三方模型", new Runnable() {
            public void run() {
                // 保留默认主题的完整编辑器，避免在经典主题里塞一个残缺表单
                act.openProviderEditor(null);
            }
        });
        sheet.show(act.root().overlay);
    }

    private void assistantSheet() {
        M3Sheet sheet = new M3Sheet(act).title("助手人设").subtitle("当前：" + Assistants.current(act).name);
        ArrayList<Assistants.Assistant> all = Assistants.all(act);
        String def = Assistants.defaultId(act);
        for (int i = 0; i < all.size(); i++) {
            final Assistants.Assistant as = all.get(i);
            sheet.addChecked(Icons.SPARKLE, as.name + " · " + as.desc, def.equals(as.id),
                    new Runnable() {
                        public void run() {
                            Assistants.setDefault(act, as.id);
                            buildRows();
                            act.toast("默认助手：" + as.name);
                        }
                    });
        }
        sheet.divider();
        sheet.add(Icons.EDIT, "全局系统提示词", new Runnable() {
            public void run() {
                M3Dialog.input(act.root().overlay, "全局系统提示词", "例如：回答简短一些",
                        Prefs.getString(act, Prefs.KEY_SYSTEM_PROMPT, ""), new M3Dialog.InputCallback() {
                            public void onInput(String text) {
                                Prefs.setString(act, Prefs.KEY_SYSTEM_PROMPT, text);
                                act.toast("已保存");
                            }
                        });
            }
        });
        sheet.show(act.root().overlay);
    }

    private void dataSheet() {
        final int sessions = SessionStore.list(act).size();
        M3Sheet sheet = new M3Sheet(act).title("数据与缓存");
        sheet.addDesc(Icons.LIST, "会话数量", sessions + " 段 · 保存在应用私有目录", null);
        sheet.addDesc(Icons.BOOK, "知识库", Knowledge.all(act).size() + " 条片段", null);
        sheet.divider();
        sheet.addDanger(Icons.TRASH, "清空全部会话", new Runnable() {
            public void run() {
                M3Dialog.confirmDanger(act.root().overlay, "清空全部会话？",
                        "所有对话记录会被删除，图片缓存保留。此操作无法恢复。", "全部清空", new Runnable() {
                            public void run() {
                                ArrayList<com.chatyue.app.data.Session> all = SessionStore.list(act);
                                for (int i = 0; i < all.size(); i++) {
                                    SessionStore.delete(act, all.get(i).id);
                                }
                                buildRows();
                                act.toast("已清空全部会话");
                            }
                        });
            }
        });
        sheet.show(act.root().overlay);
    }

    private void aboutDialog() {
        M3Dialog.alert(act.root().overlay, "ChatYUE 3.0",
                "一个最低支持 Android 1.5 的原生对话客户端。\n\n"
                        + "默认主题是手绘奶油风；经典主题是独立的 Material 3 页面，"
                        + "拥有自己的导航、列表、面板与动效。\n\n"
                        + "原项目：Muse（github.com/Zer0Qing/Muse，GPL-3.0）\n"
                        + "本应用源码：github.com/chumianyi/ChatYUE（GPL-3.0）\n\n"
                        + "没有账号体系，没有云同步，会话只在本机。");
    }

    private void helpSheet() {
        M3Sheet sheet = new M3Sheet(act).title("使用技巧");
        sheet.addDesc(Icons.EDIT, "长按消息", "复制、收藏、重新生成、编辑后重发", null);
        sheet.addDesc(Icons.PLUS, "加号", "发图、拍照、查看知识库", null);
        sheet.addDesc(Icons.PALETTE, "切换主题", "外观里点「经典」进入 M3，点其他颜色回到默认主题", null);
        sheet.addDesc(Icons.MESSAGE, "新对话", "首页 hero 卡或右上角按钮都能开新会话", null);
        sheet.show(act.root().overlay);
    }

    private void privacySheet() {
        M3Sheet sheet = new M3Sheet(act).title("隐私").subtitle("简单说：不收集、不上传");
        sheet.addDesc(Icons.LOCK, "本地存储", "会话、图片、知识库都写在应用私有目录", null);
        sheet.addDesc(Icons.GLOBE, "网络请求", "只有对话时会把当前消息发到你配置的模型端点", null);
        sheet.addDesc(Icons.INFO, "权限", "仅联网；相机与相册由系统选择器代管", null);
        sheet.show(act.root().overlay);
    }

    private String firstLetter(String name) {
        if (name == null || name.length() == 0) return "你";
        return name.substring(0, 1);
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
}
