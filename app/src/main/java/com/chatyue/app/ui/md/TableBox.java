package com.chatyue.app.ui.md;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

import java.util.ArrayList;

/** 表格：自绘网格，列宽按内容测量，超宽可横向拖动。 */
public class TableBox extends View {

    private final ArrayList<String> head;
    private final ArrayList<ArrayList<String>> rows;
    private final int[] colW;
    private final int rowH;
    private final int totalW;
    private int scrollX;
    private int maxScrollX;
    private final TextPaint cellPaint;
    private final TextPaint headPaint;
    private final Paint linePaint = Ui.fillPaint(Palette.LINE);
    private final Paint headBg = Ui.fillPaint(Palette.PINK_TINT);
    private final Paint bgPaint = Ui.fillPaint(0xFFFFFFFF);
    private float downX;
    private float lastX;

    public TableBox(Context c, ArrayList<String> head, ArrayList<ArrayList<String>> rows, float fontScale) {
        super(c);
        this.head = head;
        this.rows = rows;
        cellPaint = Ui.textPaint(Palette.INK, 12.4f * fontScale, false);
        headPaint = Ui.textPaint(Palette.INK, 12.4f * fontScale, true);
        rowH = Ui.dp(34);
        int cols = head.size();
        for (int i = 0; i < rows.size(); i++) {
            cols = Math.max(cols, rows.get(i).size());
        }
        colW = new int[Math.max(1, cols)];
        int pad = Ui.dp(10);
        for (int i = 0; i < colW.length; i++) {
            int w = Ui.dp(70);
            for (int r = -1; r < rows.size(); r++) {
                String s = cell(r, i);
                int tw = (int) cellPaint.measureText(s) + pad * 2;
                if (tw > w) w = tw;
            }
            colW[i] = Math.min(w, Ui.dp(230));
        }
        int sum = 0;
        for (int i = 0; i < colW.length; i++) sum += colW[i];
        totalW = sum;
        setWillNotDraw(false);
    }

    private String cell(int row, int col) {
        if (row < 0) return col < head.size() ? (head.get(col) == null ? "" : head.get(col)) : "";
        ArrayList<String> r = rows.get(row);
        return col < r.size() ? (r.get(col) == null ? "" : r.get(col)) : "";
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w = Ui.screenWidth() - Ui.dp(56);
        }
        maxScrollX = Math.max(0, totalW - (w - Ui.dp(4)));
        scrollX = Ui.clamp(scrollX, 0, maxScrollX);
        setMeasuredDimension(w, rowH * (rows.size() + 1) + Ui.dp(2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float rad = Ui.dpf(12);
        canvas.drawRoundRect(new RectF(0, 0, w, h), rad, rad, bgPaint);
        canvas.save();
        canvas.clipRect(new RectF(0, 0, w, h));
        int y = 0;
        for (int r = -1; r < rows.size(); r++) {
            if (r == -1) {
                canvas.drawRect(0, 0, w, rowH, headBg);
            }
            int x = Ui.dp(2) - scrollX;
            for (int cIdx = 0; cIdx < colW.length; cIdx++) {
                String s = cell(r, cIdx);
                TextPaint p = r == -1 ? headPaint : cellPaint;
                String draw = Ui.ellipsize(p, s, colW[cIdx] - Ui.dp(14));
                float base = y + rowH / 2f;
                canvas.drawText(draw, x + Ui.dp(7), Ui.baselineCentered(p, base), p);
                x += colW[cIdx];
            }
            y += rowH;
            canvas.drawRect(0, y - 1, w, y, linePaint);
        }
        // 竖线
        int x = Ui.dp(2) - scrollX;
        for (int c = 0; c < colW.length; c++) {
            canvas.drawRect(x, 0, x + 1, h, linePaint);
            x += colW[c];
        }
        canvas.restore();
        Paint border = Ui.strokePaint(Palette.LINE, 1f);
        canvas.drawRoundRect(new RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f), rad, rad, border);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                lastX = downX;
                if (maxScrollX > 0) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (maxScrollX > 0) {
                    scrollX = Ui.clamp(scrollX - (int) (e.getX() - lastX), 0, maxScrollX);
                    lastX = e.getX();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
        }
        return super.onTouchEvent(e);
    }
}
