package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/** M3 胶囊按钮：filled / tonal / outlined / text，自带按压缩放与状态层。 */
public class M3Button extends View {

    public static final int FILLED = 0;
    public static final int TONAL = 1;
    public static final int OUTLINED = 2;
    public static final int TEXT = 3;

    private final int style;
    private String text;
    private int icon;
    private float heightDp = 48f;
    private float padH = 22f;
    private float press;
    private Tween pressTween;
    private final TextPaint tp;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public M3Button(Context c, String t, int style) {
        super(c);
        this.text = t == null ? "" : t;
        this.style = style;
        int fg;
        if (style == FILLED) fg = M3.ON_PRIMARY;
        else if (style == TONAL) fg = M3.ON_PRIMARY_CONTAINER;
        else fg = M3.PRIMARY;
        tp = Ui.textPaint(fg, style == TEXT ? 14.4f : 14.2f, style == FILLED);
        setClickable(true);
    }

    public M3Button icon(int id) {
        icon = id;
        requestLayout();
        return this;
    }

    public M3Button height(int dp) {
        heightDp = dp;
        requestLayout();
        return this;
    }

    public M3Button padH(int dp) {
        padH = dp;
        requestLayout();
        return this;
    }

    public M3Button bold() {
        tp.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tw = (int) Math.ceil(tp.measureText(text));
        int iw = icon == 0 ? 0 : Ui.dp(18) + Ui.dp(8);
        int w = tw + iw + Ui.dp(padH) * 2;
        int h = Ui.dp((int) heightDp);
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode == MeasureSpec.AT_MOST) w = Math.min(w, MeasureSpec.getSize(widthMeasureSpec));
        else if (mode == MeasureSpec.EXACTLY) w = MeasureSpec.getSize(widthMeasureSpec);
        int hm = MeasureSpec.getMode(heightMeasureSpec);
        if (hm == MeasureSpec.EXACTLY) h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        float scale = 1f - 0.035f * press;
        canvas.save();
        canvas.scale(scale, scale, getWidth() / 2f, getHeight() / 2f);

        if (style == FILLED) {
            fillPaint.setColor(M3.pressed(M3.PRIMARY, press));
            canvas.drawRoundRect(rect, getHeight() / 2f, getHeight() / 2f, fillPaint);
        } else if (style == TONAL) {
            fillPaint.setColor(M3.pressed(M3.PRIMARY_CONTAINER, press));
            canvas.drawRoundRect(rect, getHeight() / 2f, getHeight() / 2f, fillPaint);
        } else if (style == OUTLINED) {
            strokePaint.setColor(M3.pressed(M3.OUTLINE, press));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(Ui.dpf(1));
            canvas.drawRoundRect(new RectF(Ui.dpf(0.5f), Ui.dpf(0.5f),
                    getWidth() - Ui.dpf(0.5f), getHeight() - Ui.dpf(0.5f)),
                    getHeight() / 2f, getHeight() / 2f, strokePaint);
        }
        if (style == TEXT && press > 0.01f) {
            overlay.setColor(Ui.alpha(0xFF000000, (int) (10 * press)));
            canvas.drawRoundRect(rect, Ui.dpf(18), Ui.dpf(18), overlay);
        }

        float contentW = tp.measureText(text) + (icon == 0 ? 0 : Ui.dp(18) + Ui.dp(8));
        float x = (getWidth() - contentW) / 2f;
        if (icon != 0) {
            Icons.draw(canvas, icon, x + Ui.dpf(9), getHeight() / 2f, Ui.dpf(18), tp.getColor(), 1.7f);
            x += Ui.dp(18) + Ui.dp(8);
        }
        canvas.drawText(text, x, Ui.baselineCentered(tp, getHeight() / 2f), tp);
        canvas.restore();
    }

    private void animatePress(final boolean down) {
        if (pressTween != null) pressTween.cancel();
        pressTween = Tween.of(press, down ? 1f : 0f, down ? 90 : 180, new Tween.Listener() {
            public void onUpdate(float v) {
                press = v;
                invalidate();
            }

            public void onEnd() {
            }
        });
        pressTween.ease(Tween.DECEL).start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isEnabled()) {
                    animatePress(true);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                animatePress(false);
                if (e.getX() >= 0 && e.getX() <= getWidth() && e.getY() >= 0 && e.getY() <= getHeight()) {
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
