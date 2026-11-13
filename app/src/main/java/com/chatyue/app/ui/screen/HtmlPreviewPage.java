package com.chatyue.app.ui.screen;

import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.content.FileSharing;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.TopBar;

import java.io.File;
import java.lang.reflect.Method;

/**
 * 本地 HTML 预览：WebView 直接打开工作目录里的 .html，
 * 同目录的 CSS / JS 会用相对路径正常加载；默认开启 JavaScript 与 DOM Storage。
 */
public class HtmlPreviewPage extends Page {

    private final File file;
    private final WebView web;
    private final Label status;

    public HtmlPreviewPage(MainActivity a, File f) {
        super(a);
        file = f;

        TopBar bar = new TopBar(a, f.getName(), "HTML 预览 · 支持 CSS / JS", Icons.MORE);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        if (bar.right != null) {
            bar.right.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    moreSheet();
                }
            });
        }
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(66)));

        status = new Label(a, "正在加载…", 11.4f, Palette.INK_3, false);
        status.setAlign(Label.CENTER);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        slp.topMargin = Ui.dp(72);
        addView(status, slp);

        web = new WebView(a);
        web.setBackgroundColor(Palette.BG);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        try {
            s.setDomStorageEnabled(true);
        } catch (Throwable ignored) {
        }
        if (Experiment.sdkInt() >= 7) {
            try {
                s.setLoadWithOverviewMode(false);
                s.setUseWideViewPort(true);
            } catch (Throwable ignored) {
            }
        }
        if (Experiment.sdkInt() >= 11) {
            try {
                s.setDisplayZoomControls(false);
            } catch (Throwable ignored) {
            }
        }
        // API 16 才有的两个开关：允许 file:// 页面加载同目录的 CSS/JS
        try {
            Method m1 = WebSettings.class.getMethod("setAllowFileAccessFromFileURLs",
                    new Class[]{Boolean.TYPE});
            m1.invoke(s, new Object[]{Boolean.TRUE});
            Method m2 = WebSettings.class.getMethod("setAllowUniversalAccessFromFileURLs",
                    new Class[]{Boolean.TYPE});
            m2.invoke(s, new Object[]{Boolean.TRUE});
        } catch (Throwable ignored) {
        }

        FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        wlp.topMargin = Ui.dp(66);
        wlp.bottomMargin = Ui.dp(56);
        addView(web, wlp);

        LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(8));
        actions.setBackgroundColor(Palette.CARD);

        SoftButton reload = new SoftButton(a, "刷新", 12.6f, Palette.INK_2, Palette.BG_DEEP);
        reload.radius(14).pad(14, 8).icon(Icons.REFRESH);
        reload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                web.reload();
            }
        });
        actions.addView(reload, new LinearLayout.LayoutParams(0, Ui.dp(40), 1f));

        SoftButton share = new SoftButton(a, "分享", 12.6f, Palette.INK_2, Palette.BG_DEEP);
        share.radius(14).pad(14, 8).icon(Icons.SHARE);
        share.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FileSharing.share(act, single());
            }
        });
        LinearLayout.LayoutParams shlp = new LinearLayout.LayoutParams(0, Ui.dp(40), 1f);
        shlp.leftMargin = Ui.dp(8);
        actions.addView(share, shlp);

        SoftButton save = new SoftButton(a, "保存", 12.6f, 0xFFFFFFFF, Palette.PINK);
        save.radius(14).pad(14, 8).icon(Icons.DOWNLOAD);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.saveArtifact(file);
            }
        });
        LinearLayout.LayoutParams savlp = new LinearLayout.LayoutParams(0, Ui.dp(40), 1f);
        savlp.leftMargin = Ui.dp(8);
        actions.addView(save, savlp);

        FrameLayout.LayoutParams alp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(56), Gravity.BOTTOM);
        addView(actions, alp);

        load();
    }

    private java.util.ArrayList<File> single() {
        java.util.ArrayList<File> list = new java.util.ArrayList<File>();
        list.add(file);
        return list;
    }

    private void load() {
        try {
            status.setText("本地文件 · " + file.getAbsolutePath());
            web.loadUrl(Uri.fromFile(file).toString());
        } catch (Throwable t) {
            status.setText("无法加载：" + t.getMessage());
        }
    }

    private void moreSheet() {
        Sheet s = new Sheet(act).title(file.getName()).subtitle("HTML 预览");
        s.add(Icons.REFRESH, "重新加载", new Runnable() {
            public void run() {
                web.reload();
            }
        });
        s.add(Icons.SHARE, "分享文件", new Runnable() {
            public void run() {
                FileSharing.share(act, single());
            }
        });
        s.add(Icons.DOWNLOAD, "保存到本地", new Runnable() {
            public void run() {
                act.saveArtifact(file);
            }
        });
        s.add(Icons.GLOBE, "用系统浏览器打开", new Runnable() {
            public void run() {
                act.openInBrowser(file);
            }
        });
        s.add(Icons.CODE, "查看源码", new Runnable() {
            public void run() {
                act.push(new TextPreviewPage(act, file));
            }
        });
        s.show(act.root().overlay);
    }

    @Override
    public void onPageHidden() {
        try {
            web.onPause();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onPageShown() {
        try {
            web.onResume();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            web.stopLoading();
            web.clearHistory();
            web.removeAllViews();
            web.destroy();
        } catch (Throwable ignored) {
        }
        super.onDetachedFromWindow();
    }
}
