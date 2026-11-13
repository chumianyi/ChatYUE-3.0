package com.chatyue.app.ui.md;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 单段富文本。用 API 1 就存在的 StaticLayout 完成排版，
 * 折行、行高、Span 着色全部交给系统，自己只负责测量与绘制。
 */
public class MdText extends View {

    protected TextPaint paint;
    protected CharSequence text;
    protected StaticLayout layout;
    protected int padH;
    protected int padV;
    protected float extraLine;

    public MdText(Context c, CharSequence t, TextPaint p, int padH, int padV, float extraLineDp) {
        super(c);
        this.text = t;
        this.paint = p;
        this.padH = padH;
        this.padV = padV;
        this.extraLine = Ui.dpf(extraLineDp);
    }

    public void setText(CharSequence t) {
        if (t == null) t = "";
        if (text != null && text.toString().equals(t.toString())) return;
        text = t;
        layout = null;
        requestLayout();
        invalidate();
    }

    public CharSequence getText() {
        return text == null ? "" : text;
    }

    public void setTextColor(int color) {
        paint.setColor(color);
        layout = null;
        invalidate();
    }

    public int contentHeight() {
        return layout == null ? 0 : layout.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w = Ui.screenWidth() - Ui.dp(48);
        }
        int avail = Math.max(1, w - padH * 2);
        layout = new StaticLayout(text == null ? "" : text, paint, avail,
                Layout.Alignment.ALIGN_NORMAL, 1.0f, extraLine, false);
        int finalW = w;
        // 单行时收缩到自然宽度，让短消息的气泡不至于被拉满整屏
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST
                && layout.getLineCount() == 1) {
            int natural = (int) Math.ceil(layout.getLineWidth(0)) + padH * 2;
            if (natural < finalW) finalW = Math.max(natural, Ui.dp(8));
        }
        setMeasuredDimension(finalW, layout.getHeight() + padV * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (layout == null) return;
        canvas.save();
        canvas.translate(padH, padV);
        layout.draw(canvas);
        canvas.restore();
    }

    protected Paint basePaint() {
        return paint;
    }

    protected int inkColor() {
        return Palette.INK;
    }
}
