package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/**
 * M3 底部导航。选中项是「粉色药丸 + 下方文字」，未选中只留图标，
 * 右上角可以带一个红底计数徽标——和参考截图一致。
 */
public class M3NavBar extends View {

    public interface Listener {
        void onSelect(int index);
    }

    public static final int[] ICONS = {Icons.MESSAGE, Icons.LIST, Icons.BOOK, Icons.STAR, Icons.MORE_H};
    private static final String[] LABELS = {"对话", "会话", "知识", "收藏", "更多"};

    private int selected;
    private Listener listener;
    private float pillCx;
    private float labelAlpha = 1f;
    private Tween moveTween;
    private Tween labelTween;
    private String badge = "";
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = Ui.textPaint(M3.ON_SURFACE, 11.4f, false);
    private final TextPaint badgeText = Ui.textPaint(0xFFFFFFFF, 8.4f, true);
    private final RectF rect = new RectF();
    private final RectF badgeRect = new RectF();

    public M3NavBar(Context c) {
        super(c);
        setClickable(true);
        badgeText.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public M3NavBar listener(Listener l) {
        listener = l;
        return this;
    }

    public M3NavBar selected(int index) {
        selected = index;
        pillCx = centerOf(index);
        labelAlpha = 1f;
        invalidate();
        return this;
    }

    public M3NavBar badge(String text) {
        badge = text == null ? "" : text;
        invalidate();
        return this;
    }

    public void select(int index, boolean animate) {
        if (index < 0 || index >= LABELS.length) return;
        final float from = centerOf(selected);
        final float to = centerOf(index);
        selected = index;
        labelAlpha = 0f;
        if (moveTween != null) moveTween.cancel();
        if (labelTween != null) labelTween.cancel();
        if (!animate) {
            pillCx = to;
            labelAlpha = 1f;
            invalidate();
            return;
        }
        moveTween = Tween.of(from, to, 220, new Tween.Listener() {
            public void onUpdate(float v) {
                pillCx = v;
                invalidate();
            }

            public void onEnd() {
                pillCx = to;
                invalidate();
            }
        });
        moveTween.ease(Tween.STANDARD).start();
        labelTween = Tween.of(0, 1, 150, new Tween.Listener() {
            public void onUpdate(float v) {
                labelAlpha = v;
                invalidate();
            }

            public void onEnd() {
                labelAlpha = 1f;
                invalidate();
            }
        }).delay(60);
        labelTween.ease(Tween.DECEL).start();
    }

    private float centerOf(int index) {
        return getWidth() * (index + 0.5f) / LABELS.length;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) w = Ui.screenWidth() - Ui.dp(24);
        setMeasuredDimension(w, Ui.dp(74));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        bg.setColor(M3.SURFACE_LOW);
        canvas.drawRoundRect(rect, Ui.dpf(28), Ui.dpf(28), bg);
        // 顶部极轻的一条高光，让悬浮条不贴死背景
        bg.setColor(Ui.alpha(0xFFFFFFFF, 90));
        canvas.drawRoundRect(new RectF(0, 0, getWidth(), Ui.dpf(26)), Ui.dpf(28), Ui.dpf(28), bg);

        int n = LABELS.length;
        float itemW = getWidth() / (float) n;
        float iconCy = Ui.dpf(29);
        if (pillCx == 0f) pillCx = centerOf(selected);
        // 选中药丸
        pill.setColor(M3.PRIMARY_CONTAINER);
        canvas.drawRoundRect(new RectF(pillCx - Ui.dpf(26), iconCy - Ui.dpf(26),
                pillCx + Ui.dpf(26), iconCy + Ui.dpf(26)), Ui.dpf(16), Ui.dpf(16), pill);

        for (int i = 0; i < n; i++) {
            float cx = itemW * (i + 0.5f);
            int color = i == selected ? M3.PRIMARY_DEEP : M3.ON_SURFACE_3;
            Icons.draw(canvas, ICONS[i], cx, iconCy, Ui.dpf(23), color, 1.7f);
            if (i == selected) {
                labelPaint.setColor(M3.ON_SURFACE);
                labelPaint.setAlpha((int) (255 * labelAlpha));
                canvas.drawText(LABELS[i], cx, Ui.baselineCentered(labelPaint, Ui.dpf(62)), labelPaint);
            }
        }

        if (badge.length() > 0) {
            float cx = itemW * 0.5f + Ui.dpf(20);
            float cy = iconCy - Ui.dpf(17);
            float tw = badgeText.measureText(badge);
            float bw = Math.max(Ui.dpf(18), tw + Ui.dpf(10));
            badgeRect.set(cx - bw / 2f, cy - Ui.dpf(9), cx + bw / 2f, cy + Ui.dpf(9));
            badgePaint.setColor(M3.BADGE);
            canvas.drawRoundRect(badgeRect, Ui.dpf(9), Ui.dpf(9), badgePaint);
            canvas.drawText(badge, cx, Ui.baselineCentered(badgeText, cy), badgeText);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            int n = LABELS.length;
            int index = (int) (e.getX() / (getWidth() / (float) n));
            if (index >= 0 && index < n) {
                if (index != selected) select(index, true);
                if (listener != null) listener.onSelect(index);
                performClick();
            }
            return true;
        }
        return true;
    }
}
