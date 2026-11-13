package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 旋转弧线加载指示器。 */
public class Spinner extends View {

    private boolean running;
    private long startAt;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int color = Palette.PINK;
    private float stroke = 2.2f;

    private final Runnable tick = new Runnable() {
        public void run() {
            if (!running) return;
            invalidate();
            postDelayed(this, 26);
        }
    };

    public Spinner(Context c) {
        super(c);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setColor(int c) {
        color = c;
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
        int size = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) size = Ui.dp(22);
        int hs = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) hs = Ui.dp(22);
        setMeasuredDimension(size, hs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float t = (SystemClock.uptimeMillis() - startAt) / 1000f;
        float angle = (t * 300f) % 360f;
        float inset = Ui.dpf(stroke) + 1;
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);
        paint.setStrokeWidth(Ui.dpf(stroke));
        paint.setColor(Ui.alpha(color, 50));
        canvas.drawArc(rect, 0, 360, false, paint);
        paint.setColor(color);
        canvas.drawArc(rect, angle, 100, false, paint);
    }
}
