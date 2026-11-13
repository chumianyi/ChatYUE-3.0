package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MorphFrame;
import com.chatyue.app.ui.widget.RoundBox;

import java.util.ArrayList;

/** M3 轻提示：深色反色表面 + 大圆角，和粉白页面形成明确层级。 */
public final class M3Snackbar {

    private static final ArrayList<String> QUEUE = new ArrayList<String>();
    private static boolean busy;

    private M3Snackbar() {
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
        Context c = layer.getContext();
        final MorphFrame morph = new MorphFrame(c);
        morph.pivot(0.5f, 0.5f);

        RoundBox box = new RoundBox(c);
        box.setFill(0xFF322728);
        box.setRadius(18);
        box.setShadow(0x332A1B22, 10);
        box.setPadding(Ui.dp(18), Ui.dp(12), Ui.dp(18), Ui.dp(12));
        Label label = new Label(c, text, 13.2f, 0xFFFFFFFF, false);
        label.setAlign(Label.CENTER);
        box.addView(label, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        morph.addView(box, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Ui.dp(100);
        lp.leftMargin = Ui.dp(24);
        lp.rightMargin = Ui.dp(24);
        final int off = Ui.dp(16);
        morph.transform(0f, 0.94f, 0f, off);
        layer.addView(morph, lp);
        morph.measure(View.MeasureSpec.makeMeasureSpec(Ui.screenWidth() - Ui.dp(48), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(Ui.dp(200), View.MeasureSpec.AT_MOST));
        Tween.of(0, 1, 190, new Tween.Listener() {
            public void onUpdate(float v) {
                morph.transform(v, 0.94f + 0.06f * v, 0f, (int) (off * (1f - v)));
            }

            public void onEnd() {
                morph.transform(1f, 1f, 0f, 0f);
            }
        }).ease(Tween.DECEL).start();

        Tween.post(new Runnable() {
            public void run() {
                Tween.of(0, 1, 180, new Tween.Listener() {
                    public void onUpdate(float v) {
                        morph.transform(1f - v, 1f - 0.03f * v, 0f, (int) (-Ui.dp(8) * v));
                    }

                    public void onEnd() {
                        layer.removeView(morph);
                        next(layer);
                    }
                }).ease(Tween.ACCEL).start();
            }
        }, 1900);
    }
}
