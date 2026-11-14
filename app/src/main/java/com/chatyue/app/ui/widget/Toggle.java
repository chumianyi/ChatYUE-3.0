package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/**
 * 自绘开关。系统没有 Switch（API 14+），这里用 Canvas 画胶囊轨道 + 圆形滑块，
 * 滑动用 Tween 补间，按压时轨道轻微变暗。
 */
public class Toggle extends View {

    public interface Listener {
        void onToggle(boolean checked);
    }

    private boolean checked;
    private float anim;
    private float press;
    private Tween animTween;
    private Listener listener;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final RectF knob = new RectF();

    public Toggle(Context c) {
        super(c);
        setClickable(true);
    }

    public Toggle listener(Listener l) {
        listener = l;
        return this;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean v) {
        setChecked(v, false);
    }

    public void setChecked(boolean v, boolean animate) {
        float target = v ? 1f : 0f;
        // 注意：不能在“状态已经等于 v”时直接 return，否则开关会被静默置位，
        // 但滑块动画/颜色没有刷新，看起来就像没有打开。
        if (!animate) {
            checked = v;
            anim = target;
            if (animTween != null) animTween.cancel();
            invalidate();
            return;
        }
        if (checked == v && Math.abs(anim - target) < 0.01f) return;
        checked = v;
        if (animTween != null) animTween.cancel();
        animTween = Tween.of(anim, target, 170, new Tween.Listener() {
            public void onUpdate(float value) {
                anim = value;
                invalidate();
            }

            public void onEnd() {
            }
        });
        animTween.ease(Tween.STANDARD).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = Ui.dp(48);
        int h = Ui.dp(30);
        int wm = MeasureSpec.getMode(widthMeasureSpec);
        if (wm == MeasureSpec.EXACTLY) w = MeasureSpec.getSize(widthMeasureSpec);
        int hm = MeasureSpec.getMode(heightMeasureSpec);
        if (hm == MeasureSpec.EXACTLY) h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float pad = Ui.dpf(2.5f);
        float th = h - pad * 2;
        float tw = Math.min(w - pad * 2, th * 1.62f);
        float left = (w - tw) / 2f;
        float top = pad;
        track.set(left, top, left + tw, top + th);

        int off = Palette.mix(Palette.INK_4, Palette.BG, 0.35f);
        int on = Palette.PINK;
        int color = Palette.mix(off, on, anim);
        if (press > 0.01f) color = Palette.mix(color, 0xFF000000, 1f - 0.06f * press);
        trackPaint.setColor(color);
        canvas.drawRoundRect(track, th / 2f, th / 2f, trackPaint);

        float kr = th / 2f - Ui.dpf(2.6f);
        float cy = top + th / 2f;
        float cx = left + th / 2f + (tw - th) * anim;
        knobShadow.setColor(0x22000000);
        knob.set(cx - kr - Ui.dpf(0.6f), cy - kr + Ui.dpf(1.2f), cx + kr + Ui.dpf(0.6f), cy + kr + Ui.dpf(2.2f));
        canvas.drawOval(knob, knobShadow);
        knobPaint.setColor(0xFFFFFFFF);
        knob.set(cx - kr, cy - kr, cx + kr, cy + kr);
        canvas.drawOval(knob, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                press = 1f;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                press = 0f;
                if (e.getX() >= 0 && e.getX() <= getWidth() && e.getY() >= 0 && e.getY() <= getHeight()) {
                    // 不要先把 checked 改掉再调用 setChecked：setChecked 会以为
                    // 状态没变而提前返回，滑块和轨道就不会重绘。
                    boolean next = !checked;
                    setChecked(next, true);
                    if (listener != null) listener.onToggle(next);
                    performClick();
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                press = 0f;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }
}
