package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Providers;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;

/** 帮助与反馈：把最常遇到的四个问题写清楚，并给出可复制的反馈模板。 */
public class HelpPage extends BasePage {

    private static final String MAIL = "support@chatyue.app";

    public HelpPage(MainActivity a) {
        super(a, "帮助与反馈", "先看这四个");
        build();
    }

    private void build() {
        heading("遇到问题先看这里", "大部分情况都能自己排查，下面的答案对应应用里的真实行为。");

        section("常见问题");
        faq("回复一直不出现？", "先确认网络可用。如果顶部提示「请求失败」，展开技术细节看 HTTP 状态码：\n\n"
                + "- 401：密钥无效，去「模型提供方」里检查\n"
                + "- 404：Base URL 少了 /v1 或多写了路径\n"
                + "- 超时：服务方限流，稍后再试\n\n"
                + "内置 ChatYUE 的地址与密钥是托管好的，不需要你填写。");
        faq("HTTPS 一直握手失败？", "老系统的 TLS 证书库太旧。本应用内置了 wolfSSL 兜底链路："
                + "若安装包里带了 libwolfssl_bridge.so，会自动切过去重试；没有带时，"
                + "错误面板会明确显示「兜底库未加载」。明文 HTTP 不受影响。");
        faq("图片发不出去？", "图片会转成 base64 塞进请求体，体积过大会被服务方拒绝。"
                + "建议先裁剪到 1MB 以内。模型不支持视觉时会忽略图片，只回答文字。");
        faq("换主题后界面没变？", "主题是在控件构造时读取的。切换主题会重建页面栈，"
                + "如果你正停留在对话页，返回首页再进来即可看到新配色。");

        section("反馈");
        CardList card = card();
        card.padding(16, 14);
        card.add(new Label(act, "写信时附上下面这段信息，能省一轮来回。", 12.4f, Palette.INK_2, false));

        final String info = "应用：ChatYUE 3.0\n"
                + "Android：" + android.os.Build.VERSION.RELEASE + "（API " + android.os.Build.VERSION.SDK + "）\n"
                + "机型：" + android.os.Build.MODEL + "\n"
                + "默认模型：" + Providers.current(act).name + " · " + Providers.current(act).model + "\n"
                + "主题：" + Prefs.theme(act) + " / 字号：" + Prefs.fontPercent(act) + "%\n"
                + "问题描述：";

        row(Icons.COPY, "复制反馈模板", "含机型、版本与模型信息", new View.OnClickListener() {
            public void onClick(View v) {
                act.copyText(info);
            }
        });
        row(Icons.DOC, "复制邮箱", MAIL, new View.OnClickListener() {
            public void onClick(View v) {
                act.copyText(MAIL);
            }
        });
        row(Icons.INFO, "查看技术细节", "构建方式与兼容性说明", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.alert(act.root().overlay, "技术细节",
                        "包名：com.chatyue.app\n"
                                + "minSdkVersion：3（Android 1.5）\n"
                                + "targetSdkVersion：30\n"
                                + "编译：JDK 8 + aapt2 + dx，DEX 035\n"
                                + "依赖：无（不引入 AndroidX / support / Material）");
            }
        });

        note("邮箱仅用于反馈，不会用于营销。工作日通常 48 小时内回复。");
        space(8);
        Label cr = new Label(act, "© 2026 ChatYUE", 11.4f, Palette.INK_4, false);
        cr.setPadding(0, Ui.dp(6), 0, 0);
        content.addView(cr, lp(20, 6, 20, 0));
    }

    private void faq(String q, final String answer) {
        com.chatyue.app.ui.widget.MenuRow r = new com.chatyue.app.ui.widget.MenuRow(
                act, com.chatyue.app.ui.widget.MenuRow.STYLE_CARD);
        r.position(false, false);
        r.icon(Icons.INFO, Palette.PINK_SOFT, Palette.PINK_DEEP);
        r.title(q);
        r.chevron(true);
        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.alert(act.root().overlay, "说明", answer);
            }
        });
        card().addRow(r);
    }
}
