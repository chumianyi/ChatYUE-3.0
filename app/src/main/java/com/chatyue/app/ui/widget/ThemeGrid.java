package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Theme;
import com.chatyue.app.core.Ui;

/**
 * 主题选择网格：3 列 × 4 行色卡，选中项有描边 + 勾。
 * 不用 GridView（低版本滚动会露出系统 overscroll 效果），直接一个 View 算坐标。
 */
public class ThemeGrid extends View {

    public interface Listener {
        void onPick(int index);
    }

    private static final int COLS = 3;
    private int selected;
    private Listener listener;

    private final Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint check = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tick = new Path();
    private final TextPaint label = Ui.textPaint(Palette.INK_2, 11.6f, false);

    public ThemeGrid(Context c) {
        super(c);
        label.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
    }

    public ThemeGrid selected(int index) {
        selected = index;
        invalidate();
        return this;
    }

    public ThemeGrid listener(Listener l) {
        listener = l;
        return this;
    }

    private int rows() {
        return (Theme.COUNT + COLS - 1) / COLS;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Ui.dp(rows() * 84));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        float cellW = w / (float) COLS;
        float cellH = Ui.dpf(84);
        for (int i = 0; i < Theme.COUNT; i++) {
            int row = i / COLS;
            int col = i % COLS;
            float cx = cellW * col + cellW / 2f;
            float cy = cellH * row + Ui.dpf(28);
            float r = Ui.dpf(21);

            circle.setColor(Theme.accent(i));
            canvas.drawCircle(cx, cy, r, circle);

            if (i == selected) {
                ring.setStyle(Paint.Style.STROKE);
                ring.setStrokeWidth(Math.max(1.6f, Ui.dpf(1.8f)));
                ring.setColor(Palette.INK);
                canvas.drawCircle(cx, cy, r + Ui.dpf(5), ring);
                check.setColor(0xFFFFFFFF);
                check.setStyle(Paint.Style.STROKE);
                check.setStrokeWidth(Ui.dpf(1.9f));
                tick.reset();
                tick.moveTo(cx - Ui.dpf(5.5f), cy + Ui.dpf(0.5f));
                tick.lineTo(cx - Ui.dpf(1.2f), cy + Ui.dpf(4.6f));
                tick.lineTo(cx + Ui.dpf(6f), cy - Ui.dpf(4.4f));
                canvas.drawPath(tick, check);
            }

            label.setColor(i == selected ? Palette.INK : Palette.INK_2);
            float ty = cy + r + Ui.dpf(19);
            canvas.drawText(Theme.name(i), cx, Ui.baselineCentered(label, ty), label);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            float cellW = getWidth() / (float) COLS;
            float cellH = Ui.dpf(84);
            int col = (int) (e.getX() / cellW);
            int row = (int) (e.getY() / cellH);
            int index = row * COLS + col;
            if (col >= 0 && col < COLS && row >= 0 && index < Theme.COUNT) {
                selected = index;
                invalidate();
                if (listener != null) listener.onPick(index);
                performClick();
            }
        }
        return true;
    }
}
