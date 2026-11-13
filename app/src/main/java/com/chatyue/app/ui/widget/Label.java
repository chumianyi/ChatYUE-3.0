package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;

import com.chatyue.app.core.Ui;

/**
 * 单行文本标签。不继承 TextView，避免任何系统默认内边距 / 字体度量带来的版式偏差。
 */
public class Label extends View {

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    private String text = "";
    private TextPaint paint;
    private int align = LEFT;
    private float letterSpace = 0f;

    public Label(Context c, String text, float sizeSp, int color, boolean bold) {
        super(c);
        this.text = text == null ? "" : text;
        this.paint = Ui.textPaint(color, sizeSp, bold);
    }

    public void setText(String t) {
        String s = t == null ? "" : t;
        if (s.equals(text)) return;
        text = s;
        requestLayout();
        invalidate();
    }

    public String getText() {
        return text;
    }

    public void setTextColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public int getTextColor() {
        return paint.getColor();
    }

    public void setTextSize(float sp) {
        paint.setTextSize(Ui.dpf(sp));
        requestLayout();
    }

    public TextPaint paint() {
        return paint;
    }

    public void setAlign(int a) {
        align = a;
        invalidate();
    }

    public void setBold(boolean bold) {
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        requestLayout();
    }

    public void setLetterSpace(float px) {
        letterSpace = px;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int spec = MeasureSpec.getSize(widthMeasureSpec);
        int tw = (int) Math.ceil(paint.measureText(text)) + (int) (letterSpace * Math.max(0, text.length() - 1));
        int w;
        if (mode == MeasureSpec.UNSPECIFIED) {
            w = tw;
        } else if (mode == MeasureSpec.AT_MOST) {
            w = Math.min(tw, spec);
        } else {
            w = spec;
        }
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int h = fm.descent - fm.ascent;
        int hm = MeasureSpec.getMode(heightMeasureSpec);
        if (hm == MeasureSpec.EXACTLY) h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float y = Ui.baselineCentered(paint, getHeight() / 2f);
        String draw = text;
        if (paint.measureText(draw) > getWidth()) {
            draw = Ui.ellipsize(paint, draw, getWidth());
        }
        float x;
        if (align == CENTER) {
            x = (getWidth() - paint.measureText(draw)) / 2f;
        } else if (align == RIGHT) {
            x = getWidth() - paint.measureText(draw);
        } else {
            x = 0;
        }
        if (letterSpace > 0f) {
            float cx = x;
            for (int i = 0; i < draw.length(); i++) {
                String ch = String.valueOf(draw.charAt(i));
                canvas.drawText(ch, cx, y, paint);
                cx += paint.measureText(ch) + letterSpace;
            }
        } else {
            canvas.drawText(draw, x, y, paint);
        }
    }
}
