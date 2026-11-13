package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.AlphaView;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/**
 * 圆形图标按钮：所有顶栏按钮、发送键、返回键统一用它。
 * 按压反馈是自绘的「缩小 + 加深」，不依赖任何系统 state drawable。
 */
public class IconButton extends View implements AlphaView {

    private int icon;
    private int bgColor;
    private int fgColor;
    private float sizeDp;
    private float iconDp;
    private float strokeDp = 2f;
    private float press;
    private Tween pressTween;
    private boolean down;
    private boolean enabled = true;
    /** 见 AlphaFrame.SAVE_FLAGS：必须带 ALL_SAVE_FLAGS，否则硬件加速下直接抛异常。 */
    private static final int SAVE_FLAGS = 31;

    private float viewAlpha = 1f;
    private final android.graphics.RectF layerRect = new android.graphics.RectF();

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public IconButton(Context c, int icon, int bg, int fg, float sizeDp, float iconDp) {
        super(c);
        this.icon = icon;
        this.bgColor = bg;
        this.fgColor = fg;
        this.sizeDp = sizeDp;
        this.iconDp = iconDp;
        setClickable(true);
    }

    public void setIcon(int icon) {
        this.icon = icon;
        invalidate();
    }

    public void setColors(int bg, int fg) {
        this.bgColor = bg;
        this.fgColor = fg;
        invalidate();
    }

    public void setIconSize(float dp) {
        this.iconDp = dp;
        invalidate();
    }

    public void setStroke(float dp, int color) {
        strokeDp = dp;
        strokePaint.setColor(color);
        strokePaint.setStyle(Paint.Style.STROKE);
        invalidate();
    }

    public void setEnabledState(boolean e) {
        enabled = e;
        setViewAlpha(e ? 1f : 0.45f);
        invalidate();
    }

    public void setViewAlpha(float a) {
        float v = a < 0f ? 0f : (a > 1f ? 1f : a);
        if (Math.abs(v - viewAlpha) > 0.001f) {
            viewAlpha = v;
            invalidate();
        }
    }

    public float getViewAlpha() {
        return viewAlpha;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Ui.dp(sizeDp);
        setMeasuredDimension(size, size);
    }

    @Override
    public void draw(Canvas canvas) {
        if (viewAlpha >= 0.998f) {
            super.draw(canvas);
            return;
        }
        if (viewAlpha <= 0.004f) return;
        layerRect.set(0, 0, getWidth(), getHeight());
        int sc = canvas.saveLayerAlpha(layerRect, (int) (viewAlpha * 255f), SAVE_FLAGS);
        super.draw(canvas);
        canvas.restoreToCount(sc);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float scale = 1f - 0.075f * press;
        float r = Math.min(cx, cy) * scale;
        bgPaint.setColor(bgColor);
        canvas.drawCircle(cx, cy, r, bgPaint);
        if (press > 0.01f) {
            bgPaint.setColor(Ui.alpha(0xFF000000, (int) (18 * press)));
            canvas.drawCircle(cx, cy, r, bgPaint);
        }
        if (strokePaint.getStrokeWidth() > 0f && strokePaint.getColor() != 0) {
            strokePaint.setStrokeWidth(Ui.dpf(strokeDp));
            canvas.drawCircle(cx, cy, r - Ui.dpf(strokeDp) / 2f, strokePaint);
        }
        Icons.draw(canvas, icon, cx, cy, Ui.dpf(iconDp), fgColor, 1.9f);
    }

    private void animatePress(final boolean isDown) {
        down = isDown;
        if (pressTween != null) pressTween.cancel();
        pressTween = Tween.of(press, isDown ? 1f : 0f, isDown ? 90 : 180,
                new Tween.Listener() {
                    public void onUpdate(float v) {
                        press = v;
                        invalidate();
                    }

                    public void onEnd() {
                    }
                });
        pressTween.ease(isDown ? Tween.DECEL : Tween.STANDARD).start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!enabled) return false;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animatePress(true);
                return true;
            case MotionEvent.ACTION_UP:
                animatePress(false);
                if (Ui.hit(new android.graphics.RectF(0, 0, getWidth(), getHeight()), e.getX(), e.getY())) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                animatePress(false);
                return true;
        }
        return super.onTouchEvent(e);
    }
}
