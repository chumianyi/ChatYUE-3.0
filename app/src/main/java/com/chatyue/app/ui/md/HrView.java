package com.chatyue.app.ui.md;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 分隔线。 */
public class HrView extends View {

    private final Paint paint = Ui.fillPaint(Palette.LINE);

    public HrView(Context c) {
        super(c);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Ui.dp(13));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float y = getHeight() / 2f;
        canvas.drawRect(Ui.dp(2), y, getWidth() - Ui.dp(2), y + Math.max(1, Ui.dpf(0.8f)), paint);
    }
}
