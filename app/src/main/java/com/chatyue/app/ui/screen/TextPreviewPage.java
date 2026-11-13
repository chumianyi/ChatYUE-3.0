package com.chatyue.app.ui.screen;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chatyue.app.MainActivity;
import com.chatyue.app.content.FileSharing;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.TopBar;
import com.chatyue.app.util.FileUtil;

import java.io.File;

/** 文本 / 代码在线预览：等宽字体、可滚动、可复制、可分享与保存。 */
public class TextPreviewPage extends Page {

    private final File file;
    private TextView body;

    public TextPreviewPage(MainActivity a, File f) {
        super(a);
        file = f;

        TopBar bar = new TopBar(a, f.getName(), "文本预览 · " + com.chatyue.app.data.AgentTools.sizeText(f.length()),
                Icons.MORE);
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

        BounceScrollView scroll = new BounceScrollView(a);
        RoundBox card = new RoundBox(a);
        card.setFill(Palette.CARD);
        card.setRadius(14);
        card.setStroke(Palette.LINE, 0.8f);
        card.setPadding(Ui.dp(13), Ui.dp(12), Ui.dp(13), Ui.dp(12));
        card.setShadow(Palette.SHADOW, 3);

        body = new TextView(a);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12.6f);
        body.setTextColor(Palette.INK);
        body.setBackgroundDrawable(null);
        body.setLineSpacing(Ui.dpf(3.2f), 1f);
        if (com.chatyue.app.data.Experiment.sdkInt() >= 11) body.setTextIsSelectable(true);
        body.setPadding(0, 0, 0, 0);
        card.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        scroll.addView(card, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        slp.topMargin = Ui.dp(70);
        slp.leftMargin = Ui.dp(14);
        slp.rightMargin = Ui.dp(14);
        slp.bottomMargin = Ui.dp(64);
        addView(scroll, slp);

        LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(8));
        actions.setBackgroundColor(Palette.CARD);

        SoftButton copy = new SoftButton(a, "复制全文", 12.6f, Palette.INK_2, Palette.BG_DEEP);
        copy.radius(14).pad(14, 8).icon(Icons.COPY);
        copy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.copyText(body.getText().toString());
            }
        });
        actions.addView(copy, new LinearLayout.LayoutParams(0, Ui.dp(40), 1f));

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

        addView(actions, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(56), Gravity.BOTTOM));

        load();
    }

    private java.util.ArrayList<File> single() {
        java.util.ArrayList<File> list = new java.util.ArrayList<File>();
        list.add(file);
        return list;
    }

    private void load() {
        try {
            long len = file.length();
            String text = FileUtil.readText(file, 1024 * 1024);
            if (len > 1024 * 1024) {
                text = text + "\n\n（文件较大，这里只预览前 1 MB，完整内容请分享或保存后查看）";
            }
            body.setText(TextUtils.isEmpty(text) ? "（空文件）" : text);
        } catch (Throwable t) {
            body.setText("无法读取为文本：" + t.getMessage()
                    + "\n\n可以点右上角「更多」分享或保存这个文件。");
        }
    }

    private void moreSheet() {
        Sheet s = new Sheet(act).title(file.getName()).subtitle("文本预览");
        s.add(Icons.COPY, "复制全文", new Runnable() {
            public void run() {
                act.copyText(body.getText().toString());
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
        s.show(act.root().overlay);
    }
}
