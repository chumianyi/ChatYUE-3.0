package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 自绘步进器：− 数值 +。用于字号、上下文轮数、温度这类离散取值。
 */
public class Stepper extends View {

    public interface Listener {
        void onValue(int value);
    }

    private int value;
    private int min;
    private int max;
    private int step = 1;
    private String suffix = "";
    private int divisor = 1;
    private int decimals;
    private Listener listener;
    private int pressed = -1;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint valuePaint = Ui.textPaint(Palette.INK, 13f, true);
    private final TextPaint signPaint = Ui.textPaint(Palette.INK_2, 17f, false);
    private final RectF rect = new RectF();

    public Stepper(Context c) {
        super(c);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        signPaint.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
    }

    public Stepper range(int minValue, int maxValue, int stepValue) {
        min = minValue;
        max = maxValue;
        step = stepValue <= 0 ? 1 : stepValue;
        return this;
    }

    /** 显示时把数值除以 divisor，并保留 decimals 位小数（如 70 -> 0.7）。 */
    public Stepper display(int divisorValue, int decimalPlaces) {
        divisor = divisorValue <= 0 ? 1 : divisorValue;
        decimals = decimalPlaces;
        return this;
    }

    public Stepper suffix(String s) {
        suffix = s == null ? "" : s;
        return this;
    }

    public Stepper listener(Listener l) {
        listener = l;
        return this;
    }

    public int value() {
        return value;
    }

    public void setValue(int v) {
        value = clamp(v);
        invalidate();
    }

    private int clamp(int v) {
        if (v < min) v = min;
        if (v > max) v = max;
        return v;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Ui.dp(112), Ui.dp(32));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        rect.set(0.6f, 0.6f, w - 0.6f, h - 0.6f);
        bg.setColor(Palette.BG_DEEP);
        canvas.drawRoundRect(rect, h / 2f, h / 2f, bg);
        line.setColor(Palette.LINE);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, Ui.dpf(0.9f)));
        canvas.drawRoundRect(rect, h / 2f, h / 2f, line);

        float third = w / 3f;
        float cy = h / 2f;
        if (pressed == 0) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Palette.LINE_2);
            canvas.drawCircle(third / 2f, cy, h / 2f - Ui.dpf(2), p);
        } else if (pressed == 1) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Palette.LINE_2);
            canvas.drawCircle(w - third / 2f, cy, h / 2f - Ui.dpf(2), p);
        }

        signPaint.setColor(value > min ? Palette.INK_2 : Palette.INK_4);
        canvas.drawText("−", third / 2f, Ui.baselineCentered(signPaint, cy), signPaint);
        signPaint.setColor(value < max ? Palette.INK_2 : Palette.INK_4);
        canvas.drawText("+", w - third / 2f, Ui.baselineCentered(signPaint, cy), signPaint);

        valuePaint.setColor(Palette.INK);
        canvas.drawText(label(), w / 2f, Ui.baselineCentered(valuePaint, cy), valuePaint);
    }

    private String label() {
        if (divisor <= 1) return value + suffix;
        int whole = value / divisor;
        if (decimals <= 0) return whole + suffix;
        int frac = Math.abs(value % divisor) * 10 / divisor;
        return whole + "." + frac + suffix;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float third = getWidth() / 3f;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = e.getX() < third ? 0 : (e.getX() > third * 2 ? 1 : -1);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                int old = pressed;
                pressed = -1;
                if (old == 0) {
                    change(-step);
                } else if (old == 1) {
                    change(step);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void change(int delta) {
        int next = clamp(value + delta);
        if (next == value) return;
        value = next;
        invalidate();
        if (listener != null) listener.onValue(value);
    }
}
