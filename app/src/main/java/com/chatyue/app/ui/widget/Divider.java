package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 卡片内的细分隔线，左侧留出图标宽度，和 iOS 风格列表一致。 */
public class Divider extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float inset = 60f;

    public Divider(Context c) {
        super(c);
        paint.setColor(Palette.LINE_2);
    }

    public Divider inset(float dp) {
        inset = dp;
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.max(1, Ui.dp(1)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Palette.LINE_2);
        float l = getWidth() > Ui.dp(inset) * 2 ? Ui.dp(inset) : 0;
        canvas.drawRect(l, 0, getWidth(), Math.max(1, Ui.dpf(0.8f)), paint);
    }
}
