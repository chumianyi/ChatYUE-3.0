package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 首页底部快捷动作：图标在上 / 文字在下的按钮，也可切换成粉色实心样式。 */
public class QuickAction extends View {

    private int icon;
    private String label;
    private int fg = Palette.INK_2;
    private int iconFg = Palette.INK_2;
    private int bg;
    private float radius = 24;
    private boolean pressed;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp;

    public QuickAction(Context c, int icon, String label) {
        super(c);
        this.icon = icon;
        this.label = label;
        tp = Ui.textPaint(fg, 11.4f, false);
    }

    public QuickAction colors(int fgColor, int iconColor, int bgColor) {
        fg = fgColor;
        iconFg = iconColor;
        bg = bgColor;
        tp.setColor(fg);
        invalidate();
        return this;
    }

    public QuickAction radius(float r) {
        radius = r;
        return this;
    }

    public void setLabel(String s) {
        label = s;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) w = Ui.dp(80);
        setMeasuredDimension(w, Ui.dp(54));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (bg != 0) {
            bgPaint.setColor(bg);
            canvas.drawRoundRect(new RectF(0, 0, w, h), Ui.dpf(radius), Ui.dpf(radius), bgPaint);
        }
        if (pressed) {
            bgPaint.setColor(Ui.alpha(0xFF000000, 14));
            canvas.drawRoundRect(new RectF(0, 0, w, h), Ui.dpf(radius), Ui.dpf(radius), bgPaint);
        }
        Icons.draw(canvas, icon, w / 2f, h / 2f - Ui.dpf(8), Ui.dpf(20), iconFg, 1.8f);
        float tw = tp.measureText(label);
        canvas.drawText(label, (w - tw) / 2f, h - Ui.dpf(9), tp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                pressed = false;
                invalidate();
                if (e.getX() >= 0 && e.getX() <= getWidth() && e.getY() >= 0 && e.getY() <= getHeight()) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }
}
