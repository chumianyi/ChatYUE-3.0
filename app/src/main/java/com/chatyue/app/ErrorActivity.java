package com.chatyue.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.util.ClipboardCompat;
import com.chatyue.app.util.FileUtil;

import java.io.File;

/**
 * 独立进程的错误页。主进程崩溃后它仍然活着，
 * 把日志完整展示出来，并提供「复制 / 重试 / 返回」。
 */
public class ErrorActivity extends Activity {

    private String log = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        Palette.apply(Prefs.theme(this));
        if (getIntent() != null) {
            log = getIntent().getStringExtra("log");
            if (log == null || log.length() == 0) {
                String path = getIntent().getStringExtra("path");
                if (path == null) path = Prefs.crashLogPath(this);
                if (path != null && path.length() > 0) {
                    try {
                        log = FileUtil.readText(new File(path));
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        if (log == null || log.length() == 0) log = "没有拿到崩溃日志。";

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.BG);
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(22), Ui.dp(38), Ui.dp(22), Ui.dp(24));

        View mark = new View(this) {
            protected void onDraw(android.graphics.Canvas canvas) {
                Icons.draw(canvas, Icons.INFO, getWidth() / 2f, getHeight() / 2f,
                        Math.min(getWidth(), getHeight()) * 0.82f, Palette.PINK_DEEP, 2f);
            }
        };
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44));
        mlp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(mark, mlp);

        Label title = new Label(this, "应用遇到了问题", 24f, Palette.INK, true);
        title.setAlign(Label.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(16);
        col.addView(title, tlp);

        Label sub = new Label(this, "别担心，会话记录都还在。日志已经保存，可以复制反馈或直接重试。",
                12.4f, Palette.INK_3, false);
        sub.setAlign(Label.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(8);
        col.addView(sub, slp);

        RoundBox card = new RoundBox(this);
        card.setFill(Palette.CARD);
        card.setRadius(14);
        card.setStroke(Palette.LINE, 0.8f);
        card.setPadding(Ui.dp(13), Ui.dp(12), Ui.dp(13), Ui.dp(12));

        TextView body = new TextView(this);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11.6f);
        body.setTextColor(Palette.INK_2);
        body.setBackgroundDrawable(null);
        body.setText(log);
        if (com.chatyue.app.data.Experiment.sdkInt() >= 11) body.setTextIsSelectable(true);
        body.setLineSpacing(Ui.dpf(2.6f), 1f);
        card.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(18);
        col.addView(card, clp);

        scroll.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        scrollLp.bottomMargin = Ui.dp(72);
        root.addView(scroll, scrollLp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(Ui.dp(18), Ui.dp(8), Ui.dp(18), Ui.dp(14));
        actions.setBackgroundColor(Palette.CARD);

        final SoftButton copy = new SoftButton(this, "复制日志", 13f, Palette.INK_2, Palette.BG_DEEP);
        copy.radius(15).pad(12, 9).icon(Icons.COPY);
        copy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardCompat.setText(ErrorActivity.this, log);
                copy.setText("已复制");
            }
        });
        actions.addView(copy, new LinearLayout.LayoutParams(0, Ui.dp(46), 1f));

        SoftButton retry = new SoftButton(this, "重试", 13f, 0xFFFFFFFF, Palette.PINK);
        retry.radius(15).pad(12, 9).icon(Icons.REFRESH);
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent(ErrorActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Throwable ignored) {
                }
                finish();
            }
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, Ui.dp(46), 1f);
        rlp.leftMargin = Ui.dp(10);
        actions.addView(retry, rlp);

        SoftButton back = new SoftButton(this, "返回", 13f, Palette.INK_2, Palette.BG_DEEP);
        back.radius(15).pad(12, 9);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, Ui.dp(46), 1f);
        blp.leftMargin = Ui.dp(10);
        actions.addView(back, blp);

        FrameLayout.LayoutParams alp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(72), Gravity.BOTTOM);
        root.addView(actions, alp);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
