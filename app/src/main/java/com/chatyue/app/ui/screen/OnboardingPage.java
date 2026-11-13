package com.chatyue.app.ui.screen;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.chatyue.app.ChatYueApp;
import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Theme;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.data.Lang;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Provider;
import com.chatyue.app.data.Providers;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.FieldBox;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.StepDots;
import com.chatyue.app.ui.widget.ThemeGrid;
import com.chatyue.app.ui.widget.Toggle;

/**
 * 新手引导：9 步，一次只问一件事。
 *
 * 结构上刻意不用 ViewPager（API 1 没有），而是「一个页面 + 每步重建内容区 + 固定底部按钮」，
 * 这样转场由内容替换完成，不会出现左右滑动的边界问题。
 */
public class OnboardingPage extends Page {

    private static final int STEPS = 9;

    private final int step;
    private final LinearLayout body;
    private final SoftButton prev;
    private final SoftButton next;
    private final StepDots dots;
    private final Label stepLabel;

    private String chosenLang;
    private String chosenName;
    private boolean chosenMemory;

    public OnboardingPage(MainActivity a) {
        this(a, 0);
    }

    public OnboardingPage(MainActivity a, int startStep) {
        super(a);
        step = startStep < 0 ? 0 : (startStep >= STEPS ? STEPS - 1 : startStep);
        chosenLang = Lang.current(a);
        chosenName = Prefs.getString(a, Prefs.KEY_NICKNAME, "");
        chosenMemory = Prefs.getBool(a, Prefs.KEY_MEMORY, true);
        setBackgroundColor(Palette.BG);

        dots = new StepDots(a).setup(STEPS, step);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(20));
        dlp.topMargin = Ui.dp(16);
        dlp.leftMargin = Ui.dp(20);
        dlp.rightMargin = Ui.dp(20);
        addView(dots, dlp);

        stepLabel = new Label(a, "第 " + (step + 1) + " 步 / 共 " + STEPS + " 步",
                11.6f, Palette.INK_3, false);
        stepLabel.setAlign(Label.RIGHT);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(18));
        tlp.topMargin = Ui.dp(38);
        tlp.rightMargin = Ui.dp(22);
        addView(stepLabel, tlp);

        BounceScrollView scroll = new BounceScrollView(a);
        body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(20), Ui.dp(6), Ui.dp(20), Ui.dp(16));
        scroll.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        slp.topMargin = Ui.dp(62);
        slp.bottomMargin = Ui.dp(92);
        addView(scroll, slp);

        LinearLayout footer = new LinearLayout(a);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(Ui.dp(20), Ui.dp(10), Ui.dp(20), Ui.dp(20));

        prev = new SoftButton(a, "← 上一步", 14f, Palette.INK_2, Palette.CARD);
        prev.stroke(Palette.LINE, 1);
        prev.radius(24);
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                back();
            }
        });

        next = new SoftButton(a, step == STEPS - 1 ? "去聊聊吧" : "下一步 →",
                14f, 0xFFFFFFFF, Palette.INK);
        next.radius(24);
        next.bold(true);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                advance();
            }
        });

        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, Ui.dp(50), 1f);
        plp.rightMargin = Ui.dp(10);
        footer.addView(prev, plp);
        footer.addView(next, new LinearLayout.LayoutParams(0, Ui.dp(50), 1.55f));
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(80));
        flp.gravity = Gravity.BOTTOM;
        addView(footer, flp);

        prev.setVisibility(step == 0 ? View.INVISIBLE : View.VISIBLE);
        buildStep();
    }

    // ------------------------------------------------------------ 步骤内容

    private void buildStep() {
        body.removeAllViews();
        switch (step) {
            case 0:
                stepWelcome();
                break;
            case 1:
                stepLanguage();
                break;
            case 2:
                stepName();
                break;
            case 3:
                stepMemory();
                break;
            case 4:
                stepModel();
                break;
            case 5:
                stepPrivacy();
                break;
            case 6:
                stepTheme();
                break;
            case 7:
                stepExperiment();
                break;
            default:
                stepReady();
                break;
        }
    }

    private Label title(String text) {
        Label l = new Label(act, text, 23f, Palette.INK, true);
        body.addView(l, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return l;
    }

    private void desc(String text) {
        Label l = new Label(act, text, 13f, Palette.INK_2, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(9);
        body.addView(l, p);
    }

    private void stepWelcome() {
        ImageView logo = new ImageView(act);
        if (ChatYueApp.logo(act) != null) logo.setImageBitmap(ChatYueApp.logo(act));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(92), Ui.dp(92));
        ilp.topMargin = Ui.dp(46);
        ilp.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(logo, ilp);

        Label name = new Label(act, "ChatYUE", 26f, Palette.INK, true);
        name.setAlign(Label.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = Ui.dp(20);
        body.addView(name, nlp);

        Label hi = new Label(act, "嗨，终于见面了。", 21f, Palette.INK, true);
        hi.setAlign(Label.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(10);
        body.addView(hi, hlp);

        Label sub = new Label(act, "我是 ChatYUE，一个能在老手机上顺畅聊天的 AI 助手。\n\n"
                + "接下来 8 步，把你的习惯告诉我。所有设置都能在「设置」里随时改。",
                13.2f, Palette.INK_2, false);
        sub.setAlign(Label.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(14);
        body.addView(sub, slp);
    }

    private void stepLanguage() {
        title("先选个语言。");
        desc("这决定模型用什么语言回答，界面文字保持中文。");
        space(14);

        CardList list = cardLike();
        for (int i = 0; i < Lang.NAMES.length; i++) {
            final String name = Lang.NAMES[i];
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(i == 0, false);
            r.icon(Icons.GLOBE, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(name);
            r.checked(name.equals(chosenLang));
            r.chevron(false);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chosenLang = name;
                    Lang.set(act, name);
                    buildStep();
                }
            });
            list.addRow(r);
        }
    }

    private void stepName() {
        title("我该怎么叫你？");
        desc("称呼会出现在回答里，也可以留空。");
        space(14);

        final FieldBox box = new FieldBox(act, "称呼", "例如：小林", chosenName, 1);
        body.addView(box, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        chosenBox = box;

        Label tip = new Label(act, "只保存在本机，不会上传。", 11.8f, Palette.INK_3, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(10);
        body.addView(tip, p);
    }

    private FieldBox chosenBox;

    private void stepMemory() {
        title("让我记住你。");
        desc("开启后，提问会带上最近的历史消息与本机知识库。上下文越长，消耗越高。");
        space(14);

        CardList card = cardLike();
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(Icons.BOOK, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title("启用记忆");
        r.desc("对话更有连续性；关闭后只发送当前这一句");
        r.chevron(false);
        final Toggle t = new Toggle(act);
        t.setChecked(chosenMemory, true);
        t.listener(new Toggle.Listener() {
            public void onToggle(boolean checked) {
                chosenMemory = checked;
                Prefs.setBool(act, Prefs.KEY_MEMORY, checked);
            }
        });
        r.trailing(t);
        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t.setChecked(!t.isChecked(), true);
                chosenMemory = t.isChecked();
                Prefs.setBool(act, Prefs.KEY_MEMORY, chosenMemory);
            }
        });
        card.addRow(r);
    }

    private void stepModel() {
        title("模型已经配好了。");
        desc("ChatYUE 使用内置的云端模型，地址与密钥由服务端托管，你不需要填写。");
        space(14);

        Provider p = Providers.current(act);
        CardList card = cardLike();
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(Icons.SPARKLE, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(p.name);
        r.desc("当前模型 · " + p.model);
        r.checked(true);
        r.chevron(false);
        card.addRow(r);

        Label note = new Label(act, "本应用没有本地模型，全部推理都在云端完成；"
                + "离线时无法对话，但历史记录仍可查看。", 12.2f, Palette.INK_3, false);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p2.topMargin = Ui.dp(12);
        body.addView(note, p2);

        SoftButton add = new SoftButton(act, "添加第三方模型", 13.2f, Palette.INK_2, Palette.CARD);
        add.stroke(Palette.LINE, 1);
        add.radius(14);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new ProviderEditPage(act, null));
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(44));
        alp.topMargin = Ui.dp(14);
        body.addView(add, alp);
    }

    private void stepPrivacy() {
        title("先说清楚隐私。");
        desc("没有账号，没有云端同步。对话存在这台手机里。");
        space(10);

        CardList card = cardLike();
        card.padding(16, 14);
        card.add(bullet("只有你点发送时，内容才会发给模型服务方"));
        card.add(bullet("知识库、画像、快捷消息只存在本机"));
        card.add(bullet("卸载应用会删除全部会话与设置"));
        card.add(bullet("第三方模型的密钥以明文存于私有目录"));

        MenuRow more = new MenuRow(act, MenuRow.STYLE_CARD);
        more.position(false, false);
        more.icon(Icons.LOCK, Palette.PINK_SOFT, Palette.PINK_DEEP);
        more.title("阅读完整隐私政策");
        more.chevron(true);
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new PrivacyPage(act));
            }
        });
        cardLike().addRow(more);
    }

    private void stepTheme() {
        title("换个喜欢的模样。");
        desc("13 套配色（含经典 M3），之后可在设置里随时更换。");
        space(8);

        ThemeGrid grid = new ThemeGrid(act);
        grid.selected(Prefs.theme(act)).listener(new ThemeGrid.Listener() {
            public void onPick(int index) {
                Prefs.setTheme(act, index);
                Palette.apply(index);
                // 颜色在控件构造时读取，重建本页才能看到效果
                act.resetTo(new OnboardingPage(act, step));
            }
        });
        CardList card = cardLike();
        card.padding(8, 10);
        card.add(grid);
        note("当前：" + Theme.name(Prefs.theme(act)));
    }

    private void stepExperiment() {
        title("要不要试试实验室？");
        desc("三个实验性功能，默认全部关闭；之后可以在「设置 → 实验室」里随时改。");
        space(12);

        CardList card = cardLike();
        MenuRow r1 = new MenuRow(act, MenuRow.STYLE_CARD);
        r1.position(true, false);
        r1.icon(Icons.SPARKLE, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r1.title("反思");
        r1.desc("答错时多思考一次，先诚恳认错再修复");
        r1.chevron(false);
        final Toggle t1 = new Toggle(act);
        t1.setChecked(Prefs.reflectionEnabled(act));
        t1.listener(new Toggle.Listener() {
            public void onToggle(boolean checked) {
                Prefs.setReflectionEnabled(act, checked);
            }
        });
        r1.trailing(t1);
        r1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t1.setChecked(!t1.isChecked(), true);
                Prefs.setReflectionEnabled(act, t1.isChecked());
            }
        });
        card.addRow(r1);

        MenuRow r2 = new MenuRow(act, MenuRow.STYLE_CARD);
        r2.position(false, false);
        r2.icon(Icons.ROBOT, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r2.title("Agent 多轮任务");
        r2.desc("多次思考、调用文件工具、创建与预览 HTML");
        r2.chevron(false);
        final Toggle t2 = new Toggle(act);
        t2.setChecked(Prefs.agentEnabled(act));
        t2.listener(new Toggle.Listener() {
            public void onToggle(boolean checked) {
                Prefs.setAgentEnabled(act, checked);
                if (!checked) Prefs.setAllFilesEnabled(act, false);
            }
        });
        r2.trailing(t2);
        r2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t2.setChecked(!t2.isChecked(), true);
                Prefs.setAgentEnabled(act, t2.isChecked());
                if (!t2.isChecked()) Prefs.setAllFilesEnabled(act, false);
            }
        });
        card.addRow(r2);

        MenuRow r3 = new MenuRow(act, MenuRow.STYLE_CARD);
        r3.position(false, true);
        r3.icon(Icons.FOLDER, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r3.title("让 Agent 操控所有文件");
        r3.desc(Experiment.supportsAllFiles()
                ? "需要存储/所有文件权限，建议装好后在设置里开启"
                : "仅支持 Android 5.0 以上，本机不可用");
        r3.chevron(Experiment.supportsAllFiles());
        r3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new ExperimentalPage(act));
            }
        });
        card.addRow(r3);

        Label tip = new Label(act, "实验性功能不会影响普通聊天；全部文件权限可以随时在设置里收回。",
                11.6f, Palette.INK_3, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(12);
        body.addView(tip, p);
    }

    private void stepReady() {
        title("准备好了。");
        desc("接下来可以试试这些。");
        space(12);

        CardList card = cardLike();
        card.add(feature(Icons.EDIT, "日常聊天", "文字、图片都能发，回复逐字出现"));
        card.add(feature(Icons.USER, "助手与记忆", "四套人设，记得你说过的话"));
        card.add(feature(Icons.BOOK, "知识与技能", "把固定事实写进知识库，随问随用"));
        card.add(feature(Icons.DOC, "导出与收藏", "重要回答可以收藏，整段会话能导出"));
        card.add(feature(Icons.ROBOT, "实验功能", "反思、Agent 多轮任务、全部文件"));

        Label last = new Label(act, "以上都能在设置里改。现在，去说第一句话吧。",
                12.4f, Palette.INK_3, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(16);
        body.addView(last, p);
    }

    // ------------------------------------------------------------ 交互

    private void advance() {
        if (step == 1) Lang.set(act, chosenLang);
        if (step == 2 && chosenBox != null) {
            Prefs.setString(act, Prefs.KEY_NICKNAME, chosenBox.text());
        }
        if (step >= STEPS - 1) {
            Prefs.setOnboarded(act, true);
            act.showHome();
            act.toast("欢迎使用 ChatYUE");
            return;
        }
        act.resetTo(new OnboardingPage(act, step + 1));
    }

    private void back() {
        if (step == 0) return;
        act.resetTo(new OnboardingPage(act, step - 1));
    }

    @Override
    public boolean onBack() {
        if (step > 0) {
            back();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------ 小工具

    private CardList cardLike() {
        CardList card = new CardList(act);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(4);
        body.addView(card, p);
        return card;
    }

    private Label bullet(String text) {
        Label l = new Label(act, "·  " + text, 12.6f, Palette.INK_2, false);
        l.setPadding(0, Ui.dp(5), 0, Ui.dp(5));
        return l;
    }

    private MenuRow feature(int icon, String title, String desc) {
        MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(icon, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(title);
        r.desc(desc);
        r.chevron(false);
        return r;
    }

    private void note(String text) {
        Label l = new Label(act, text, 11.6f, Palette.INK_3, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(10);
        body.addView(l, p);
    }

    private void space(int dp) {
        View v = new View(act);
        body.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(dp)));
    }
}
