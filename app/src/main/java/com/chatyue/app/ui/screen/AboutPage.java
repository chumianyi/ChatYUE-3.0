package com.chatyue.app.ui.screen;

import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.chatyue.app.ChatYueApp;
import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Assistants;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.widget.Label;

/** 关于：版本、原项目、本应用源码、文档、技术与致谢。 */
public class AboutPage extends BasePage {

    private static final String MUSE = "https://github.com/Zer0Qing/Muse";
    private static final String SELF = "https://github.com/chumianyi/ChatYUE-3.0";

    public AboutPage(MainActivity a) {
        super(a, "关于", "ChatYUE 3.0");

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView logo = new ImageView(act);
        if (ChatYueApp.logo(act) != null) logo.setImageBitmap(ChatYueApp.logo(act));
        head.addView(logo, new LinearLayout.LayoutParams(Ui.dp(74), Ui.dp(74)));

        Label name = new Label(act, "ChatYUE", 22f, Palette.INK, true);
        name.setAlign(Label.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = Ui.dp(12);
        head.addView(name, nlp);

        Label ver = new Label(act, "3.0 · minSdk 3（Android 1.5）", 12.2f, Palette.INK_3, false);
        ver.setAlign(Label.CENTER);
        head.addView(ver, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Label slogan = new Label(act, "一个能在老机器上顺畅聊天的 AI 助手", 12.6f, Palette.INK_2, false);
        slogan.setAlign(Label.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(6);
        head.addView(slogan, slp);

        content.addView(head, lp(16, 14, 16, 18));

        section("项目");
        row(Icons.BOOK, "原项目 · Muse", "Zer0Qing · GPL-3.0 · " + MUSE, new View.OnClickListener() {
            public void onClick(View v) {
                act.openLink(MUSE);
            }
        });
        row(Icons.CODE, "本应用开源代码", "chumianyi/ChatYUE-3.0 · GPL-3.0 · " + SELF, new View.OnClickListener() {
            public void onClick(View v) {
                act.openLink(SELF);
            }
        });
        row(Icons.SPARKLE, "更新日志", "3.0 新增了什么", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new ChangelogPage(act));
            }
        });
        note("ChatYUE 3.0 是基于 Muse 继续开发的衍生作品，按 GNU GPL v3 发布。"
                + "再分发时请保留原作者署名与同样的许可证。");

        section("文档与协议");
        row(Icons.EDIT, "用户协议", "使用本应用即表示同意", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new TermsPage(act));
            }
        });
        row(Icons.LOCK, "隐私政策", "数据怎么存、发给谁", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new PrivacyPage(act));
            }
        });
        row(Icons.INFO, "帮助与反馈", "常见问题与反馈模板", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new HelpPage(act));
            }
        });

        section("技术");
        techCard();
        note("没有 AndroidX、Jetpack、Material、support 库；没有 Fragment、RecyclerView、"
                + "属性动画；所有界面用 View + Canvas 手绘。");

        section("致谢");
        row(Icons.STAR, "致谢与许可证", "原项目、运行时组件、wolfSSL 与手绘资产", new View.OnClickListener() {
            public void onClick(View v) {
                act.push(new LicensesPage(act));
            }
        });
        note("点击「致谢与许可证」可以看到本次使用的全部组件及其许可证。");

        primaryButton("复制版本信息", new View.OnClickListener() {
            public void onClick(View v) {
                act.copyText("ChatYUE 3.0\nminSdkVersion 3 / targetSdk 30\n模型："
                        + Prefs.getString(act, Prefs.KEY_LAST_MODEL, "GLM-4.1V-Thinking-Flash")
                        + "\n助手：" + Assistants.current(act).name);
            }
        });
    }

    private void techCard() {
        com.chatyue.app.ui.widget.CardList card = newCard(0);
        card.padding(16, 14);
        card.add(new Label(act, "语言：Java（源码级 1.6 语法，JDK 8 编译）\n"
                + "构建：aapt2 + javac + dx/d8，手写构建脚本\n"
                + "网络：HttpURLConnection + 手写 SSE 解析；HTTPS 走平台 TLS，"
                + "失败时尝试 wolfSSL 兜底\n"
                + "存储：应用私有目录 + SharedPreferences", 12.2f, Palette.INK_2, false));
    }
}
