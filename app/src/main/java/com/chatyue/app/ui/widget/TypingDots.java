package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 等待中的三个跳动小点（API 3 没有任何加载动画组件，逐帧自绘）。 */
public class TypingDots extends View {

    private boolean running;
    private long startAt;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = Palette.INK_3;
    private float radiusDp = 2.6f;

    private final Runnable tick = new Runnable() {
        public void run() {
            if (!running) return;
            invalidate();
            postDelayed(this, 33);
        }
    };

    public TypingDots(Context c) {
        super(c);
    }

    public void setColor(int c) {
        color = c;
        invalidate();
    }

    public void setRadius(float dp) {
        radiusDp = dp;
        invalidate();
    }

    public void start() {
        if (running) return;
        running = true;
        startAt = SystemClock.uptimeMillis();
        post(tick);
    }

    public void stop() {
        running = false;
        removeCallbacks(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int d = Ui.dp(radiusDp * 2 + 14);
        setMeasuredDimension(d, Ui.dp(radiusDp * 2 + 6));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float t = (SystemClock.uptimeMillis() - startAt) / 1000f;
        paint.setColor(color);
        float cy = getHeight() / 2f;
        for (int i = 0; i < 3; i++) {
            float phase = t * 5f - i * 0.7f;
            float lift = (float) Math.sin(phase) * Ui.dpf(2.6f);
            int a = 90 + (int) (120 * (0.5f + 0.5f * Math.sin(phase)));
            paint.setColor(Ui.alpha(color, Math.min(255, a)));
            canvas.drawCircle(Ui.dpf(radiusDp + 3) + i * Ui.dpf(radiusDp * 2 + 3), cy + lift,
                    Ui.dpf(radiusDp), paint);
        }
    }
}
