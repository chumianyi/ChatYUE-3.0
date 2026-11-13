package com.chatyue.app.ui.md;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.md.CodeHi;

/**
 * 代码块：自绘卡片 + 语言标签 + 一键复制 + 语法高亮 + 双向滚动。
 *
 * 没有 HorizontalScrollView 之外的现成能力可用，这里把横向滚动、竖向限高滚动
 * 与复制按钮的命中判定全部自己实现，避免嵌套系统滚动的默认外观。
 */
public class CodeBox extends ViewGroup {

    public interface CopyListener {
        void onCopy(String code);
    }

    public interface PreviewListener {
        void onPreview(String code);
    }

    private String code;
    private String lang;
    private TextPaint tp;
    private int[] colors;
    private String[] lines;
    private int[] offsets;
    private float lineH;
    private int headH;
    private int padOuter;
    private int padInner;
    private int maxBodyH;
    private int contentW;
    private int contentH;
    private int bodyH;
    private int scrollX;
    private int scrollY;
    private int maxScrollX;
    private int maxScrollY;

    private final RectF copyRect = new RectF();
    private final RectF previewRect = new RectF();
    private final RectF bubbleRect = new RectF();
    private final RectF bodyRect = new RectF();

    private boolean copyDown;
    private boolean previewDown;
    private boolean copyHot;
    private boolean dragging;
    private float downX;
    private float downY;
    private int lastX;
    private int lastY;

    private final Paint bgPaint = Ui.fillPaint(Palette.CODE_BG);
    private final Paint headPaint = Ui.fillPaint(Palette.CODE_HEAD);
    private final Paint chipPaint = Ui.fillPaint(0xFFFFFFFF);
    private final Paint chipDownPaint = Ui.fillPaint(Palette.PINK_SOFT);
    private final Paint linePaint = Ui.fillPaint(Palette.LINE);
    private final Paint codePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint langPaint;
    private final TextPaint chipText;
    private final Path headPath = new Path();
    private final Path bgPath = new Path();
    private CopyListener listener;
    private PreviewListener previewListener;

    public CodeBox(Context c, String lang, String code, float fontScale) {
        super(c);
        this.lang = lang == null ? "" : lang;
        this.code = code == null ? "" : code.replace("\t", "    ");
        float size = 12.6f * fontScale;
        tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextSize(Ui.dpf(size));
        tp.setTypeface(Typeface.MONOSPACE);
        tp.setColor(Palette.INK);
        codePaint.setTextSize(Ui.dpf(size));
        codePaint.setTypeface(Typeface.MONOSPACE);
        langPaint = Ui.textPaint(Palette.INK_3, 10.2f, true);
        chipText = Ui.textPaint(Palette.INK_2, 10.6f, false);
        headH = Ui.dp(34);
        padOuter = Ui.dp(1);
        padInner = Ui.dp(12);
        maxBodyH = Ui.dp(300);
        colors = CodeHi.run(this.code, this.lang, Palette.INK);
        splitLines();
        computeContent();
        setWillNotDraw(false);
    }

    public void setCopyListener(CopyListener l) {
        this.listener = l;
    }

    public void setPreviewListener(PreviewListener l) {
        this.previewListener = l;
        invalidate();
    }

    private boolean isHtml() {
        String l = lang == null ? "" : lang.toLowerCase();
        return l.indexOf("html") >= 0 || l.indexOf("htm") >= 0;
    }

    public String getCode() {
        return code;
    }

    private void splitLines() {
        String[] raw = code.split("\n", -1);
        lines = raw;
        offsets = new int[raw.length];
        int acc = 0;
        for (int i = 0; i < raw.length; i++) {
            offsets[i] = acc;
            acc += raw[i].length() + 1;
        }
        Paint.FontMetricsInt fm = tp.getFontMetricsInt();
        lineH = (fm.descent - fm.ascent) + Ui.dpf(4.5f);
    }

    private void computeContent() {
        float maxW = 0;
        for (int i = 0; i < lines.length; i++) {
            float w = tp.measureText(lines[i]);
            if (w > maxW) maxW = w;
        }
        contentW = (int) (maxW + padInner * 2);
        contentH = (int) (lines.length * lineH + padInner * 1.4f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w = Ui.screenWidth() - Ui.dp(56);
        }
        bodyH = Math.min(contentH, maxBodyH);
        int availW = w - padOuter * 2 - padInner * 2;
        maxScrollX = Math.max(0, contentW - availW - padInner);
        maxScrollY = Math.max(0, contentH - bodyH);
        scrollX = Ui.clamp(scrollX, 0, maxScrollX);
        scrollY = Ui.clamp(scrollY, 0, maxScrollY);
        setMeasuredDimension(w, headH + bodyH + padOuter);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // 无子 View，纯 Canvas 绘制
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float rad = Ui.dpf(13);
        bubbleRect.set(0, 0, w, h);
        bgPath.reset();
        canvas.drawRoundRect(bubbleRect, rad, rad, bgPaint);

        headPath.reset();
        headPath.addRoundRect(new RectF(0, 0, w, headH), new float[]{
                rad, rad, rad, rad, 0, 0, 0, 0}, Path.Direction.CW);
        canvas.drawPath(headPath, headPaint);
        canvas.drawRect(0, headH - 1, w, headH, linePaint);

        String label = lang.length() == 0 ? "CODE" : lang.toUpperCase();
        float ty = Ui.baselineCentered(langPaint, headH / 2f);
        canvas.drawText(label, padInner + Ui.dp(2), ty, langPaint);

        // 复制按钮
        float chipW = Ui.dp(62);
        float chipH = Ui.dp(23);
        copyRect.set(w - padInner - chipW, (headH - chipH) / 2f, w - padInner, (headH + chipH) / 2f);
        canvas.drawRoundRect(copyRect, chipH / 2f, chipH / 2f, copyDown ? chipDownPaint : chipPaint);
        float iconCx = copyRect.left + Ui.dp(15);
        Icons.draw(canvas, Icons.COPY, iconCx, copyRect.centerY(), Ui.dpf(12), Palette.INK_2, 1.4f);
        float tx = copyRect.left + Ui.dp(26);
        canvas.drawText("复制", tx, Ui.baselineCentered(chipText, copyRect.centerY()), chipText);

        if (previewListener != null && isHtml()) {
            previewRect.set(copyRect.left - Ui.dp(68), copyRect.top,
                    copyRect.left - Ui.dp(6), copyRect.bottom);
            canvas.drawRoundRect(previewRect, chipH / 2f, chipH / 2f,
                    previewDown ? chipDownPaint : chipPaint);
            Icons.draw(canvas, Icons.GLOBE, previewRect.left + Ui.dp(15), previewRect.centerY(),
                    Ui.dpf(12), Palette.PINK_DEEP, 1.4f);
            canvas.drawText("预览", previewRect.left + Ui.dp(26),
                    Ui.baselineCentered(chipText, previewRect.centerY()), chipText);
        }

        // 代码区
        bodyRect.set(padOuter, headH, w - padOuter, h - padOuter);
        canvas.save();
        canvas.clipRect(bodyRect);
        Paint.FontMetricsInt fm = tp.getFontMetricsInt();
        float baseTop = headH + padInner * 0.7f - scrollY;
        for (int i = 0; i < lines.length; i++) {
            float baseline = baseTop + i * lineH - fm.ascent;
            if (baseline < headH - lineH || baseline > h + lineH) continue;
            float x = padInner - scrollX;
            String line = lines[i];
            int off = offsets[i];
            int k = 0;
            while (k < line.length()) {
                int color = colors[off + k];
                int e = k + 1;
                while (e < line.length() && colors[off + e] == color) e++;
                codePaint.setColor(color);
                String seg = line.substring(k, e);
                canvas.drawText(seg, x, baseline, codePaint);
                x += codePaint.measureText(seg);
                k = e;
            }
        }
        canvas.restore();

        // 滚动指示
        if (maxScrollY > 0) {
            float trackTop = headH + Ui.dp(4);
            float trackH = bodyH - Ui.dp(8);
            float thumbH = Math.max(Ui.dpf(18), trackH * bodyH / (float) contentH);
            float t = maxScrollY == 0 ? 0 : scrollY / (float) maxScrollY;
            float thumbTop = trackTop + (trackH - thumbH) * t;
            Paint p = Ui.fillPaint(Ui.alpha(Palette.INK_4, 90));
            canvas.drawRoundRect(new RectF(w - Ui.dpf(3), thumbTop, w - Ui.dpf(1),
                    thumbTop + thumbH), Ui.dpf(1), Ui.dpf(1), p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                lastX = (int) x;
                lastY = (int) y;
                dragging = false;
                copyDown = Ui.hit(copyRect, x, y);
                previewDown = previewListener != null && Ui.hit(previewRect, x, y);
                copyHot = copyDown;
                invalidate();
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_MOVE: {
                int dx = (int) x - lastX;
                int dy = (int) y - lastY;
                if (!dragging && (Math.abs(x - downX) > Ui.dpf(4) || Math.abs(y - downY) > Ui.dpf(4))) {
                    dragging = true;
                    copyDown = false;
                    previewDown = false;
                }
                if (dragging) {
                    boolean horizontal = Math.abs(x - downX) > Math.abs(y - downY);
                    if (horizontal && maxScrollX > 0) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        scrollX = Ui.clamp(scrollX - dx, 0, maxScrollX);
                        invalidate();
                    } else if (maxScrollY > 0 && !horizontal) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        scrollY = Ui.clamp(scrollY - dy, 0, maxScrollY);
                        invalidate();
                    }
                }
                lastX = (int) x;
                lastY = (int) y;
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (copyDown && Ui.hit(copyRect, x, y) && listener != null) {
                    listener.onCopy(code);
                }
                if (previewDown && Ui.hit(previewRect, x, y) && previewListener != null) {
                    previewListener.onPreview(code);
                }
                copyDown = false;
                previewDown = false;
                copyHot = false;
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                copyDown = false;
                previewDown = false;
                copyHot = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    public boolean isCopyHot() {
        return copyHot;
    }
}
