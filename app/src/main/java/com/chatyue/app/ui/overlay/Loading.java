package com.chatyue.app.ui.overlay;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.Spinner;

/** 轻量阻塞加载层（只在需要等待的少数场景使用，例如拉取会话列表）。 */
public final class Loading {

    private static FrameLayout currentLayer;
    private static RoundBox current;

    private Loading() {
    }

    public static void show(FrameLayout layer, String text) {
        hide();
        if (layer == null) return;
        currentLayer = layer;
        Context c = layer.getContext();
        RoundBox box = new RoundBox(c);
        box.setFill(0xF0201E22);
        box.setRadius(18);
        box.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(20), Ui.dp(14));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Spinner sp = new Spinner(c);
        sp.setColor(0xFFFFFFFF);
        row.addView(sp, new LinearLayout.LayoutParams(Ui.dp(20), Ui.dp(20)));
        sp.start();

        Label label = new Label(c, text == null ? "加载中…" : text, 13.2f, 0xFFFFFFFF, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(10);
        row.addView(label, lp);
        box.addView(row);
        Ui.setAlpha(box, 0f);

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        layer.addView(box, flp);
        current = box;
        Tween.of(0, 255, 180, new Tween.Listener() {
            public void onUpdate(float v) {
                Ui.setAlpha(current, v / 255f);
            }

            public void onEnd() {
            }
        }).start();
    }

    public static void hide() {
        if (current != null && currentLayer != null) {
            currentLayer.removeView(current);
        }
        current = null;
        currentLayer = null;
    }
}
