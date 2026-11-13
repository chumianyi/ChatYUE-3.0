package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/** M3 开关：轨道与滑块一起做 160ms 的缓动，不是瞬间跳变。 */
public class M3Switch extends View {

    public interface Listener {
        void onChanged(boolean checked);
    }

    private boolean checked;
    private float progress;
    private Tween tween;
    private Listener listener;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public M3Switch(Context c) {
        super(c);
        setClickable(true);
    }

    public M3Switch checked(boolean c) {
        checked = c;
        progress = c ? 1f : 0f;
        invalidate();
        return this;
    }

    public boolean isChecked() {
        return checked;
    }

    public M3Switch listener(Listener l) {
        listener = l;
        return this;
    }

    public void toggle() {
        setChecked(!checked, true);
        if (listener != null) listener.onChanged(checked);
    }

    public void setChecked(boolean c, boolean animate) {
        if (checked == c) return;
        checked = c;
        if (tween != null) tween.cancel();
        if (!animate) {
            progress = c ? 1f : 0f;
            invalidate();
            return;
        }
        tween = Tween.of(progress, c ? 1f : 0f, 160, new Tween.Listener() {
            public void onUpdate(float v) {
                progress = v;
                invalidate();
            }

            public void onEnd() {
            }
        });
        tween.ease(Tween.STANDARD).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Ui.dp(50), Ui.dp(32));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        rect.set(0, 0, w, h);
        track.setColor(Ui.mix(M3.SURFACE_CONTAINER, M3.PRIMARY, progress));
        canvas.drawRoundRect(rect, h / 2f, h / 2f, track);
        if (progress < 0.15f) {
            outline.setColor(M3.OUTLINE);
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(Ui.dpf(1));
            canvas.drawRoundRect(new RectF(Ui.dpf(0.5f), Ui.dpf(0.5f),
                    w - Ui.dpf(0.5f), h - Ui.dpf(0.5f)), h / 2f, h / 2f, outline);
        }
        float r = Ui.dpf(12);
        float cx = Ui.dpf(4) + r + (w - Ui.dp(8) - r * 2) * progress;
        float cy = h / 2f;
        // 被按下时滑块轻微放大，补足材质感
        float rr = r * (1f + 0.05f * Math.abs(progress - 0.5f) * 2f);
        thumb.setColor(progress > 0.55f ? M3.ON_PRIMARY : M3.SURFACE);
        canvas.drawCircle(cx, cy, rr, thumb);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            toggle();
            performClick();
            return true;
        }
        return true;
    }
}
