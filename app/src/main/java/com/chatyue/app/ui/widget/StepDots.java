package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 引导页顶部的进度点：当前项是一段短胶囊，其余是浅色小圆点。 */
public class StepDots extends View {

    private int count = 8;
    private int current;
    private final Paint on = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint off = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StepDots(Context c) {
        super(c);
    }

    public StepDots setup(int countValue, int currentValue) {
        count = countValue;
        current = currentValue;
        invalidate();
        return this;
    }

    public void setCurrent(int index) {
        current = index;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Ui.dp(20));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float gap = Ui.dpf(9);
        float r = Ui.dpf(2.6f);
        float activeW = Ui.dpf(13);
        float total = activeW + r * 2 * (count - 1) + gap * (count - 1);
        float x = (getWidth() - total) / 2f;
        float cy = getHeight() / 2f;

        for (int i = 0; i < count; i++) {
            if (i == current) {
                on.setColor(Palette.INK);
                canvas.drawRoundRect(new android.graphics.RectF(x, cy - r, x + activeW, cy + r),
                        r, r, on);
                x += activeW + gap;
            } else {
                off.setColor(Palette.INK_4);
                canvas.drawCircle(x + r, cy, r, off);
                x += r * 2 + gap;
            }
        }
    }
}
