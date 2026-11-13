package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;

/** 致谢与开源许可：列出原项目、运行时组件、手绘资产及其许可证。 */
public class LicensesPage extends BasePage {

    private static final String MUSE = "https://github.com/Zer0Qing/Muse";
    private static final String SELF = "https://github.com/chumianyi/ChatYUE-3.0";
    private static final String GPL = "https://www.gnu.org/licenses/gpl-3.0.txt";

    public LicensesPage(MainActivity a) {
        super(a, "致谢与许可证", "所有组件 · 许可证");
        build();
    }

    private void build() {
        heading("谢谢这些开源工作",
                "ChatYUE 3.0 没有引入 AndroidX、Jetpack、Material 或 support 库；"
                        + "下面列出原项目、运行时组件，以及 APK 中可能包含的本地库。");

        section("原项目与本次代码");
        row(Icons.BOOK, "Muse · 原项目", "Zer0Qing · GPL-3.0 · 界面与交互灵感来源",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        act.openLink(MUSE);
                    }
                });
        row(Icons.CODE, "ChatYUE 3.0 · 本应用源码", "chumianyi/ChatYUE-3.0 · GPL-3.0", new View.OnClickListener() {
            public void onClick(View v) {
                act.openLink(SELF);
            }
        });
        note("本应用是在 Muse 的基础上继续开发的衍生作品，因此同样以 GNU GPL v3 发布。"
                + "你可以查看、修改、再分发源码，但再分发时需保留同样的许可证与原作者署名。");

        section("运行时与系统组件");
        row(Icons.SERVER, "Android Framework / SDK", "Apache License 2.0 及 Android 各组件许可",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        act.openLink("https://source.android.com/");
                    }
                });
        row(Icons.DOC, "org.json", "JSON License（Android 平台内置，未额外打包）",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        act.openLink("https://www.json.org/license.html");
                    }
                });
        row(Icons.LOCK, "wolfSSL（可选本地库）", "GPL-2.0 或 wolfSSL 商业许可",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        act.openLink("https://www.wolfssl.com/license/");
                    }
                });
        note("如果安装包里带有 libwolfssl_bridge.so，它使用 wolfSSL 提供 TLS 1.2/1.3 兜底；"
                + "wolfSSL 源码与许可证见其官网。没有打包时，HTTPS 会自动回落到系统 TLS。");

        section("界面资产");
        row(Icons.BRUSH, "图标", "全部由 Icons.java 用 Path 手绘，无第三方图标库", null);
        row(Icons.DOC, "字体", "使用设备系统字体（sans-serif / monospace），未内嵌字体文件", null);
        row(Icons.PALETTE, "视觉参考", "Material 3 设计规范（仅参考设计语言，未使用其代码）", null);

        section("特别感谢");
        note("感谢 Zer0Qing 的 Muse 项目打下的页面栈、手绘控件与低版本兼容思路；"
                + "感谢 wolfSSL 让老设备也能握手现代 TLS；"
                + "也感谢每一位在旧手机上继续折腾的人。");

        primaryButton("查看 GPL-3.0 全文", new View.OnClickListener() {
            public void onClick(View v) {
                act.openLink(GPL);
            }
        });
    }
}
