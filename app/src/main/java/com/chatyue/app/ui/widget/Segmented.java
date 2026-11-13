package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/**
 * 顶部分段控件（任务 / Agent / 群聊）。选中态是一个会滑动的粉色胶囊，
 * 用自绘 + 补间实现，没有任何系统 Tab 控件的外观。
 */
public class Segmented extends View {

    public interface OnSelect {
        void onSelect(int index);
    }

    private String[] items;
    private int selected;
    private float pos;
    private Tween slide;
    private OnSelect listener;
    private final TextPaint normal = Ui.textPaint(Palette.INK_2, 13.2f, false);
    private final TextPaint active = Ui.textPaint(0xFFFFFFFF, 13.2f, true);
    private final Paint bgPaint = Ui.fillPaint(0xFFFFFFFF);
    private final Paint pillPaint = Ui.fillPaint(Palette.PINK);
    private final RectF rect = new RectF();
    private final RectF pill = new RectF();
    private float downX;
    private int downIndex = -1;

    public Segmented(Context c, String[] items) {
        super(c);
        this.items = items;
        pos = 0;
    }

    public void setListener(OnSelect l) {
        listener = l;
    }

    public int selected() {
        return selected;
    }

    public void setSelected(int index, boolean animate) {
        if (index < 0 || index >= items.length) return;
        selected = index;
        if (!animate) {
            pos = index;
            invalidate();
            return;
        }
        if (slide != null) slide.cancel();
        slide = Tween.of(pos, (float) index, 260, new Tween.Listener() {
            public void onUpdate(float v) {
                pos = v;
                invalidate();
            }

            public void onEnd() {
            }
        });
        slide.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w = Ui.dp(210);
        }
        setMeasuredDimension(w, Ui.dp(40));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        float r = getHeight() / 2f;
        canvas.drawRoundRect(rect, r, r, bgPaint);
        float itemW = getWidth() / (float) items.length;
        float pad = Ui.dpf(3);
        pill.set(pad + pos * itemW, pad, pad + pos * itemW + itemW - pad * 2, getHeight() - pad);
        canvas.drawRoundRect(pill, (getHeight() - pad * 2) / 2f, (getHeight() - pad * 2) / 2f, pillPaint);
        for (int i = 0; i < items.length; i++) {
            TextPaint p = i == selected ? active : normal;
            float tw = p.measureText(items[i]);
            float x = i * itemW + (itemW - tw) / 2f;
            canvas.drawText(items[i], x, Ui.baselineCentered(p, getHeight() / 2f), p);
        }
    }

    private int indexAt(float x) {
        float itemW = getWidth() / (float) items.length;
        int i = (int) (x / itemW);
        return Ui.clamp(i, 0, items.length - 1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downIndex = indexAt(downX);
                return true;
            case MotionEvent.ACTION_UP:
                int idx = indexAt(e.getX());
                if (idx == downIndex && Math.abs(e.getX() - downX) < Ui.dpf(30)) {
                    setSelected(idx, true);
                    if (listener != null) listener.onSelect(idx);
                }
                return true;
        }
        return super.onTouchEvent(e);
    }
}
