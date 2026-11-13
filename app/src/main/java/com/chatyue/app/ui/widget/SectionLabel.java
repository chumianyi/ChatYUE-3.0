package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 分组小标题（列表里用作 section header）。 */
public class SectionLabel extends View {

    private final TextPaint tp = Ui.textPaint(Palette.INK_2, 13.6f, false);
    private String text;

    public SectionLabel(Context c, String text) {
        super(c);
        this.text = text;
    }

    public void setText(String t) {
        text = t;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) w = Ui.screenWidth();
        setMeasuredDimension(w, Ui.dp(42));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawText(text == null ? "" : text, Ui.dp(20), getHeight() - Ui.dpf(12), tp);
    }
}
