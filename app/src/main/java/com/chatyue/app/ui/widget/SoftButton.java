package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/** 胶囊文字按钮：对话框操作、建议词、取消按钮…… */
public class SoftButton extends View {

    private String text;
    private float sizeSp;
    private int fg;
    private int bg;
    private float padH;
    private float padV;
    private float radiusDp;
    private int icon;
    private float iconDp = 14;
    private float press;
    private Tween tween;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int strokeColor;
    private float strokeW;
    private android.text.TextPaint tp;
    private float maxWidth = -1;

    public SoftButton(Context c, String text, float sizeSp, int fg, int bg) {
        super(c);
        this.text = text == null ? "" : text;
        this.sizeSp = sizeSp;
        this.fg = fg;
        this.bg = bg;
        this.padH = 16;
        this.padV = 9;
        this.radiusDp = 14;
        tp = Ui.textPaint(fg, sizeSp, false);
        setClickable(true);
    }

    public SoftButton pad(float h, float v) {
        padH = h;
        padV = v;
        requestLayout();
        return this;
    }

    public SoftButton radius(float r) {
        radiusDp = r;
        invalidate();
        return this;
    }

    public SoftButton bold(boolean b) {
        tp.setTypeface(b ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        requestLayout();
        return this;
    }

    public SoftButton icon(int id) {
        icon = id;
        requestLayout();
        return this;
    }

    public SoftButton stroke(int color, float w) {
        strokeColor = color;
        strokeW = w;
        invalidate();
        return this;
    }

    public SoftButton maxWidth(float dp) {
        maxWidth = Ui.dpf(dp);
        requestLayout();
        return this;
    }

    public void setText(String t) {
        text = t == null ? "" : t;
        requestLayout();
        invalidate();
    }

    public String getText() {
        return text;
    }

    public void setColors(int fgColor, int bgColor) {
        fg = fgColor;
        bg = bgColor;
        tp.setColor(fgColor);
        invalidate();
    }

    public void setTextSize(float sp) {
        sizeSp = sp;
        tp.setTextSize(Ui.dpf(sp));
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tw = (int) Math.ceil(tp.measureText(text));
        if (maxWidth > 0 && tw > maxWidth) tw = (int) maxWidth;
        int iconW = icon == 0 ? 0 : Ui.dp(iconDp) + Ui.dp(6);
        int w = tw + iconW + Ui.dp(padH) * 2;
        int h = (int) (tp.getTextSize() * 1.35f) + Ui.dp(padV) * 2;
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode == MeasureSpec.AT_MOST) w = Math.min(w, MeasureSpec.getSize(widthMeasureSpec));
        else if (mode == MeasureSpec.EXACTLY) w = MeasureSpec.getSize(widthMeasureSpec);
        int hm = MeasureSpec.getMode(heightMeasureSpec);
        if (hm == MeasureSpec.EXACTLY) h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        RectF r = new RectF(0, 0, getWidth(), getHeight());
        if (bg != 0) {
            bgPaint.setColor(bg);
            canvas.drawRoundRect(r, Ui.dpf(radiusDp), Ui.dpf(radiusDp), bgPaint);
        }
        if (strokeColor != 0) {
            strokePaint.setColor(strokeColor);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(Ui.dpf(strokeW));
            canvas.drawRoundRect(new RectF(Ui.dpf(strokeW / 2f), Ui.dpf(strokeW / 2f),
                    getWidth() - Ui.dpf(strokeW / 2f), getHeight() - Ui.dpf(strokeW / 2f)),
                    Ui.dpf(radiusDp), Ui.dpf(radiusDp), strokePaint);
        }
        if (press > 0.01f) {
            bgPaint.setColor(Ui.alpha(0xFF000000, (int) (16 * press)));
            canvas.drawRoundRect(r, Ui.dpf(radiusDp), Ui.dpf(radiusDp), bgPaint);
        }
        float contentW = tp.measureText(text) + (icon == 0 ? 0 : Ui.dp(iconDp) + Ui.dp(6));
        float startX = (getWidth() - contentW) / 2f;
        if (icon != 0) {
            Icons.draw(canvas, icon, startX + Ui.dpf(iconDp) / 2f, getHeight() / 2f,
                    Ui.dpf(iconDp), fg, 1.8f);
            startX += Ui.dp(iconDp) + Ui.dp(6);
        }
        canvas.drawText(text, startX, Ui.baselineCentered(tp, getHeight() / 2f), tp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animate(1f, 90);
                return true;
            case MotionEvent.ACTION_UP:
                animate(0f, 180);
                if (e.getX() >= 0 && e.getX() <= getWidth() && e.getY() >= 0 && e.getY() <= getHeight()) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                animate(0f, 160);
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void animate(final float target, int dur) {
        if (tween != null) tween.cancel();
        tween = Tween.of(press, target, dur, new Tween.Listener() {
            public void onUpdate(float v) {
                press = v;
                invalidate();
            }

            public void onEnd() {
            }
        }).start();
    }
}
