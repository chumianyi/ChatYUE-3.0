package com.chatyue.app.ui.screen;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import com.chatyue.app.core.Theme;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Agents;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.AgentFlags;
import com.chatyue.app.data.Assistants;
import com.chatyue.app.data.Lang;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Provider;
import com.chatyue.app.data.Providers;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.SectionLabel;
import com.chatyue.app.util.TimeUtil;

import java.util.ArrayList;

/**
 * 设置页。整个页面就是一个 ListView + 自定义 Adapter，
 * 行样式、分组圆角、分隔线全部由 MenuRow 自绘。
 */
public class SettingsPage extends Page {

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_ROW = 1;

    private static final class Item {
        int type;
        String id;
        String title;
        String desc;
        String value;
        int icon;
        String letter;
        boolean first = true;
        boolean last = true;
        boolean divider;
    }

    private final ArrayList<Item> items = new ArrayList<Item>();
    private ListView list;
    private Adapter adapter;
    private String filter = "";

    public SettingsPage(MainActivity a) {
        super(a);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        FrameLayout header = new FrameLayout(a);
        IconButton back = new IconButton(a, Icons.BACK, 0, Palette.INK, 38, 20);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(Ui.dp(38), Ui.dp(38),
                Gravity.LEFT);
        blp.leftMargin = Ui.dp(12);
        blp.topMargin = Ui.dp(8);
        header.addView(back, blp);

        Label title = new Label(a, "设置", 28f, Palette.INK, true);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.LEFT | Gravity.BOTTOM);
        tlp.leftMargin = Ui.dp(18);
        tlp.bottomMargin = Ui.dp(8);
        header.addView(title, tlp);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(104)));

        list = new ListView(a);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(new ColorDrawable(Color.TRANSPARENT));
        list.setCacheColorHint(0);
        list.setVerticalScrollBarEnabled(false);
        list.setPadding(0, 0, 0, Ui.dp(24));
        list.setClipToPadding(false);
        adapter = new Adapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        buildItems();
    }

    @Override
    public void onPageShown() {
        buildItems();
    }

    // ------------------------------------------------------------- 数据装配

    private void buildItems() {
        items.clear();
        String kw = filter == null ? "" : filter.trim();
        if (kw.length() > 0) {
            Item search = row("search", Icons.SEARCH, "搜索设置项…", kw, "", true, true);
            items.add(search);
            addFiltered(kw);
            adapter.notifyDataSetChanged();
            return;
        }

        items.add(row("search", Icons.SEARCH, "搜索设置项…", "或全局搜索会话内容", "", true, true));

        section("助手与人设");
        groupStart();
        items.add(row("profile", Icons.USER, "用户画像", "称呼 / 身份 / 偏好 / 表达风格",
                Prefs.getString(act, Prefs.KEY_NICKNAME, "")));
        items.add(row("assistant", Icons.SPARKLE, "助手与角色", "人设 · 系统提示词 · 开场白",
                Assistants.current(act).name));
        items.add(row("agent", Icons.ROBOT, "Agent", "能力开关 · 人设入口",
                AgentFlags.enabledCount(act) + " 项"));
        int lab = (Prefs.reflectionEnabled(act) ? 1 : 0) + (Prefs.agentEnabled(act) ? 1 : 0)
                + (Prefs.allFilesEnabled(act) ? 1 : 0);
        items.add(row("lab", Icons.SPARKLE, "实验室", "反思 · 多轮任务 · 全部文件",
                lab + " 项开启"));
        items.add(row("terminal", Icons.TERMINAL, "终端", "Android 系统 shell · shell / Python 探测",
                ""));
        Item assets = row("assets", Icons.STAR, "助手资源", "收藏 / 知识库 / 模板 / 技能 / 助手", "");
        assets.last = true;
        items.add(assets);

        section("对话与表达");
        groupStart();
        items.add(row("chat", Icons.LIST, "聊天", "字号 · 记忆 · 回车发送 · 上下文", Prefs.fontLabel(act)));
        items.add(row("look", Icons.BRUSH, "外观", "13 套主题 · 实时预览", Theme.name(Prefs.theme(act))));
        items.add(row("lang", Icons.GLOBE, "回答语言", "模型用什么语言回答", Lang.current(act)));
        Item params = row("params", Icons.SPARKLE, "模型参数", "温度 · Top P · 单次上限",
                String.valueOf(Prefs.temperature(act) / 100f));
        params.last = true;
        items.add(params);

        section("模型提供方");
        ArrayList<Provider> all = Providers.all(act);
        String defId = Providers.currentId(act);
        for (int i = 0; i < all.size(); i++) {
            final Provider p = all.get(i);
            Item it = new Item();
            it.type = TYPE_ROW;
            it.id = "p:" + p.id;
            it.title = p.name;
            if (p.isBuiltin()) {
                it.letter = "C";
                it.desc = "内置 · " + p.model;
            } else {
                it.icon = Icons.ROBOT;
                it.desc = p.model + " · 第三方";
            }
            it.value = p.id.equals(defId) ? "默认" : "";
            it.first = i == 0;
            it.last = false;
            it.divider = true;
            items.add(it);
        }
        Item add = row("p_add", Icons.PLUS, "添加第三方模型", "自定义 Base URL / API Key / 模型名", "");
        add.first = all.isEmpty();
        add.last = true;
        items.add(add);

        section("数据");
        groupStart();
        items.add(row("sessions", Icons.CLOCK, "全部会话", SessionStore.list(act).size() + " 个会话", ""));
        Item data = row("data", Icons.DOC, "数据与存储", "占用 · 清理 · 重新引导", "");
        data.last = true;
        items.add(data);

        section("关于");
        groupStart();
        items.add(row("about", Icons.INFO, "关于 ChatYUE", "3.0 · 最低支持 Android 1.5", ""));
        items.add(row("help", Icons.INFO, "帮助与反馈", "常见问题 · 反馈模板", ""));
        items.add(row("terms", Icons.DOC, "用户协议", "使用条款", ""));
        Item privacy = row("privacy", Icons.LOCK, "隐私政策", "数据怎么存、发给谁", "");
        privacy.last = true;
        items.add(privacy);

        adapter.notifyDataSetChanged();
    }

    private void addFiltered(String kw) {
        ArrayList<Item> all = new ArrayList<Item>();
        all.add(row("profile", Icons.USER, "用户画像", "称呼 / 偏好 / 沟通习惯", ""));
        all.add(row("assistant", Icons.SPARKLE, "助手", "管理助手、模型、角色设定与世界书", ""));
        all.add(row("agent", Icons.ROBOT, "Agent", "助手选择 / 协作 / 主动消息", ""));
        all.add(row("lab", Icons.SPARKLE, "实验室", "反思 / 多轮任务 / 全部文件", ""));
        all.add(row("terminal", Icons.TERMINAL, "终端", "Android 系统 shell / 开发命令 / Python", ""));
        all.add(row("assets", Icons.STAR, "助手资源", "收藏夹 / 世界书 / 快捷消息 / 技能", ""));
        all.add(row("chat", Icons.LIST, "聊天", "字体大小 · 记忆 · 流式输出", ""));
        all.add(row("look", Icons.BRUSH, "外观", "浅色主题 · 圆角 · 自绘控件", ""));
        all.add(row("sessions", Icons.CLOCK, "全部会话", "历史记录管理", ""));
        all.add(row("about", Icons.INFO, "关于 ChatYUE", "版本与说明", ""));
        ArrayList<Provider> ps = Providers.all(act);
        for (int i = 0; i < ps.size(); i++) {
            Provider p = ps.get(i);
            Item it = row("p:" + p.id, p.isBuiltin() ? 0 : Icons.ROBOT, p.name,
                    p.isBuiltin() ? "内置 · " + p.model : p.model + " · 第三方", "");
            if (p.isBuiltin()) it.letter = "C";
            all.add(it);
        }
        all.add(row("p_add", Icons.PLUS, "添加第三方模型", "自定义 Base URL / API Key", ""));
        for (int i = 0; i < all.size(); i++) {
            Item it = all.get(i);
            if ((it.title != null && it.title.indexOf(kw) >= 0)
                    || (it.desc != null && it.desc.indexOf(kw) >= 0)) {
                it.first = true;
                it.last = true;
                items.add(it);
            }
        }
    }

    private void groupStart() {
    }

    private void section(String title) {
        Item it = new Item();
        it.type = TYPE_SECTION;
        it.title = title;
        items.add(it);
    }

    private Item row(String id, int icon, String title, String desc, String value) {
        return row(id, icon, title, desc, value, true, true);
    }

    private Item row(String id, int icon, String title, String desc, String value,
                     boolean first, boolean last) {
        Item it = new Item();
        it.type = TYPE_ROW;
        it.id = id;
        it.icon = icon;
        it.title = title;
        it.desc = desc;
        it.value = value;
        it.first = first;
        it.last = last;
        return it;
    }

    // ---------------------------------------------------------------- 交互

    private void onRow(String id) {
        if ("search".equals(id)) {
            act.push(new SearchPage(act));
        } else if ("profile".equals(id)) {
            act.push(new ProfilePage(act));
        } else if ("assistant".equals(id)) {
            act.push(new AssistantListPage(act));
        } else if ("agent".equals(id)) {
            act.push(new AgentPage(act));
        } else if ("lab".equals(id)) {
            act.push(new ExperimentalPage(act));
        } else if ("terminal".equals(id)) {
            act.openTerminal();
        } else if ("assets".equals(id)) {
            act.push(new AssetsPage(act));
        } else if ("chat".equals(id)) {
            act.push(new ChatSettingsPage(act));
        } else if ("look".equals(id)) {
            act.push(new AppearancePage(act));
        } else if ("lang".equals(id)) {
            act.push(new LanguagePage(act));
        } else if ("params".equals(id)) {
            act.push(new ModelParamsPage(act));
        } else if ("data".equals(id)) {
            act.push(new DataPage(act));
        } else if ("sessions".equals(id)) {
            act.push(new SessionListPage(act));
        } else if ("about".equals(id)) {
            act.push(new AboutPage(act));
        } else if ("help".equals(id)) {
            act.push(new HelpPage(act));
        } else if ("terms".equals(id)) {
            act.push(new TermsPage(act));
        } else if ("privacy".equals(id)) {
            act.push(new PrivacyPage(act));
        } else if ("p_add".equals(id)) {
            act.openProviderEditor(null);
        } else if (id != null && id.startsWith("p:")) {
            providerSheet(id.substring(2));
        }
    }

    private void assistantSheet() {
        Sheet s = new Sheet(act).title("助手").subtitle("当前：" + Providers.current(act).name);
        s.add(Icons.EDIT, "全局系统提示词", new Runnable() {
            public void run() {
                Dialog.input(act.root().overlay, "全局系统提示词", "例如：回答简短一些",
                        Prefs.getString(act, Prefs.KEY_SYSTEM_PROMPT, ""),
                        new Dialog.InputCallback() {
                            public void onInput(String text) {
                                Prefs.setString(act, Prefs.KEY_SYSTEM_PROMPT, text);
                                act.toast("已保存");
                            }
                        });
            }
        });
        s.addChecked(Icons.DOT, "参考记忆", Prefs.getBool(act, Prefs.KEY_MEMORY, true), new Runnable() {
            public void run() {
                boolean next = !Prefs.getBool(act, Prefs.KEY_MEMORY, true);
                Prefs.setBool(act, Prefs.KEY_MEMORY, next);
                act.toast(next ? "已开启记忆" : "已关闭记忆");
            }
        });
        s.add(Icons.ROBOT, "选择默认模型", new Runnable() {
            public void run() {
                modelSheet();
            }
        });
        s.show(act.root().overlay);
    }

    private void modelSheet() {
        Sheet s = new Sheet(act).title("默认对话模型");
        ArrayList<Provider> all = Providers.all(act);
        String defId = Providers.currentId(act);
        for (int i = 0; i < all.size(); i++) {
            final Provider p = all.get(i);
            s.addChecked(p.isBuiltin() ? Icons.SPARKLE : Icons.ROBOT,
                    p.name + " · " + p.model, p.id.equals(defId), new Runnable() {
                        public void run() {
                            Providers.setDefault(act, p.id);
                            buildItems();
                            act.toast("默认模型：" + p.name);
                        }
                    });
        }
        s.show(act.root().overlay);
    }

    private void agentSheet() {
        Sheet s = new Sheet(act).title("Agent").subtitle("选一个人设开新会话");
        ArrayList<Agents.Agent> list = Agents.all();
        for (int i = 0; i < list.size(); i++) {
            final Agents.Agent a = list.get(i);
            s.addDesc(a.icon, a.name, a.desc, new Runnable() {
                public void run() {
                    act.openNewChat(a.name, a.prompt);
                }
            });
        }
        s.show(act.root().overlay);
    }

    private void chatSheet() {
        final Sheet s = new Sheet(act).title("聊天");
        s.addDesc(Icons.EDIT, "字体大小", "当前：" + Prefs.fontLabel(act), new Runnable() {
            public void run() {
                Prefs.cycleFont(act);
                buildItems();
                act.toast("字号：" + Prefs.fontLabel(act) + "（重开对话生效）");
            }
        });
        s.addChecked(Icons.DOT, "参考记忆", Prefs.getBool(act, Prefs.KEY_MEMORY, true), new Runnable() {
            public void run() {
                boolean next = !Prefs.getBool(act, Prefs.KEY_MEMORY, true);
                Prefs.setBool(act, Prefs.KEY_MEMORY, next);
                act.toast(next ? "已开启记忆" : "已关闭记忆");
            }
        });
        s.addDesc(Icons.WAIT, "流式速度", "逐字输出，40ms 合批刷新", new Runnable() {
            public void run() {
                act.toast("流式输出固定开启");
            }
        });
        s.show(act.root().overlay);
    }

    private void sessionsSheet() {
        Sheet s = new Sheet(act).title("全部会话");
        ArrayList<Session> all = SessionStore.list(act);
        if (all.isEmpty()) {
            s.add(Icons.DOT, "还没有会话", null);
        }
        for (int i = 0; i < all.size() && i < 12; i++) {
            final Session meta = all.get(i);
            s.addDesc(Icons.CLOCK, meta.title,
                    TimeUtil.friendly(meta.updatedAt) + " · " + meta.preview(), new Runnable() {
                        public void run() {
                            Session full = SessionStore.load(act, meta.id);
                            if (full != null) act.openChat(full);
                        }
                    });
        }
        s.show(act.root().overlay);
    }

    private void providerSheet(final String providerId) {
        final Provider p = Providers.find(act, providerId);
        Sheet s = new Sheet(act).title(p.name);
        if (p.isBuiltin()) {
            s.subtitle("内置模型 · " + p.model);
            s.addChecked(Icons.CHECK, "设为默认模型", p.id.equals(Providers.currentId(act)),
                    new Runnable() {
                        public void run() {
                            Providers.setDefault(act, p.id);
                            buildItems();
                            act.toast("已设为默认");
                        }
                    });
            s.addDesc(Icons.LOCK, "服务由 ChatYUE 托管",
                    "地址与密钥已内置，不展示也不可编辑", null);
        } else {
            s.subtitle(p.model);
            s.addChecked(Icons.CHECK, "设为默认模型", p.id.equals(Providers.currentId(act)),
                    new Runnable() {
                        public void run() {
                            Providers.setDefault(act, p.id);
                            buildItems();
                            act.toast("已设为默认");
                        }
                    });
            s.add(Icons.EDIT, "编辑配置", new Runnable() {
                public void run() {
                    act.openProviderEditor(p.id);
                }
            });
            s.addDanger(Icons.TRASH, "删除该提供方", new Runnable() {
                public void run() {
                    Providers.remove(act, p.id);
                    buildItems();
                    act.toast("已删除");
                }
            });
        }
        s.show(act.root().overlay);
    }

    // ---------------------------------------------------------------- 适配器

    private class Adapter extends BaseAdapter {
        public int getCount() {
            return items.size();
        }

        public Object getItem(int position) {
            return items.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public int getItemViewType(int position) {
            return items.get(position).type;
        }

        public boolean isEnabled(int position) {
            return items.get(position).type == TYPE_ROW;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Item it = items.get(position);
            if (it.type == TYPE_SECTION) {
                SectionLabel sl = convertView instanceof SectionLabel
                        ? (SectionLabel) convertView : new SectionLabel(act, it.title);
                sl.setText(it.title);
                return sl;
            }
            RowWrap wrap;
            if (convertView instanceof RowWrap) {
                wrap = (RowWrap) convertView;
            } else {
                wrap = new RowWrap(act);
            }
            MenuRow row = wrap.row;
            row.position(it.first, it.last);
            row.divider(it.divider);
            row.title(it.title).desc(it.desc).value(it.value).danger(false);
            if (it.letter != null) {
                row.letter(it.letter, Palette.PINK_SOFT, Palette.PINK_DEEP);
            } else {
                row.icon(it.icon, Palette.PINK_SOFT, Palette.PINK_DEEP);
            }
            final String id = it.id;
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onRow(id);
                }
            });
            return wrap;
        }
    }

    /** ListView 的行容器：负责左右留白，卡片本身由 MenuRow 绘制。 */
    private class RowWrap extends FrameLayout {
        final MenuRow row;

        RowWrap(android.content.Context c) {
            super(c);
            setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
            row = new MenuRow(c, MenuRow.STYLE_CARD);
            row.chevron(true);
            addView(row, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        }
    }
}
