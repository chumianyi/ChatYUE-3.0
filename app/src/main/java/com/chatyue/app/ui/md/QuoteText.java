package com.chatyue.app.ui.md;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 引用块：左侧一条粉色竖线 + 缩进的灰字。 */
public class QuoteText extends MdText {

    private final Paint bar = Ui.fillPaint(Palette.PINK_LINE);
    private final RectF barRect = new RectF();

    public QuoteText(Context c, CharSequence t, TextPaint p) {
        super(c, t, p, Ui.dp(16), Ui.dp(7), 3f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        barRect.set(Ui.dp(4), Ui.dp(4), Ui.dp(7), getHeight() - Ui.dp(4));
        canvas.drawRoundRect(barRect, Ui.dpf(2), Ui.dpf(2), bar);
        super.onDraw(canvas);
    }
}
