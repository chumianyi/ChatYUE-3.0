package com.chatyue.app.ui.overlay;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;

import java.util.ArrayList;

/**
 * 自绘轻提示。系统 Toast 的默认外观被完全替代：深色胶囊、上浮淡入、自动排队。
 */
public final class Toast {

    private static final ArrayList<String> QUEUE = new ArrayList<String>();
    private static boolean busy;

    private Toast() {
    }

    public static void reset() {
        QUEUE.clear();
        busy = false;
    }

    public static void show(final FrameLayout layer, String text) {
        if (text == null || text.length() == 0) return;
        QUEUE.add(text);
        if (!busy) {
            busy = true;
            next(layer);
        }
    }

    private static void next(final FrameLayout layer) {
        if (QUEUE.isEmpty() || layer == null) {
            busy = false;
            return;
        }
        final String text = QUEUE.remove(0);
        final Context c = layer.getContext();
        final RoundBox box = new RoundBox(c);
        box.setFill(0xF0201E22);
        box.setRadius(18);
        box.setPadding(Ui.dp(16), Ui.dp(10), Ui.dp(16), Ui.dp(10));
        Label label = new Label(c, text, 13.2f, 0xFFFFFFFF, false);
        label.setAlign(Label.CENTER);
        box.addView(label, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Ui.dp(104);
        lp.leftMargin = Ui.dp(24);
        lp.rightMargin = Ui.dp(24);
        layer.addView(box, lp);
        Ui.setAlpha(box, 0f);
        box.measure(View.MeasureSpec.makeMeasureSpec(Ui.screenWidth() - Ui.dp(48), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(Ui.dp(200), View.MeasureSpec.AT_MOST));
        final int h = box.getMeasuredHeight() > 0 ? box.getMeasuredHeight() : Ui.dp(40);
        box.offsetTopAndBottom(Ui.dp(18));

        Tween.of(0, 1, 220, new Tween.Listener() {
            public void onUpdate(float v) {
                Ui.setAlpha(box, v);
                box.offsetTopAndBottom((int) (Ui.dp(18) * (1f - v)));
            }

            public void onEnd() {
            }
        }).ease(Tween.DECEL).start();

        Tween.post(new Runnable() {
            public void run() {
                Tween.of(0, 1, 200, new Tween.Listener() {
                    public void onUpdate(float v) {
                        Ui.setAlpha(box, 1f - v);
                        box.offsetTopAndBottom((int) (-Ui.dp(10) * v));
                    }

                    public void onEnd() {
                        layer.removeView(box);
                        next(layer);
                    }
                }).ease(Tween.ACCEL).start();
            }
        }, 2100);
    }

    public static void dismissAll(final FrameLayout layer) {
        QUEUE.clear();
        busy = false;
        if (layer != null) layer.removeAllViews();
    }
}
