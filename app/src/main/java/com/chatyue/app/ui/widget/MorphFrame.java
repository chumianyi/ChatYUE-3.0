package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.widget.FrameLayout;

import com.chatyue.app.core.AlphaView;

/**
 * 带「缩放 + 位移 + 透明度」的容器，用于页面转场和浮层弹出。
 *
 * API 3 没有 View.setScaleX / setTranslationY / setAlpha，这里统一在 draw 阶段
 * 用 Canvas 变换实现。pivot 取 0..1 的比例，浮层用 (0.5, 1) 就能得到
 * “从底部最小处长大”的观感。
 */
public class MorphFrame extends FrameLayout implements AlphaView {

    private static final int SAVE_FLAGS = 31;

    private float viewAlpha = 1f;
    private float scale = 1f;
    private float transX;
    private float transY;
    private float pivotX = 0.5f;
    private float pivotY = 1f;
    private final RectF layerRect = new RectF();

    public MorphFrame(Context c) {
        super(c);
        setWillNotDraw(false);
    }

    public MorphFrame pivot(float x, float y) {
        pivotX = x;
        pivotY = y;
        invalidate();
        return this;
    }

    public void transform(float alpha, float sc, float tx, float ty) {
        float a = alpha < 0f ? 0f : (alpha > 1f ? 1f : alpha);
        if (Math.abs(a - viewAlpha) < 0.001f && Math.abs(sc - scale) < 0.0005f
                && Math.abs(tx - transX) < 0.5f && Math.abs(ty - transY) < 0.5f) {
            return;
        }
        viewAlpha = a;
        scale = sc;
        transX = tx;
        transY = ty;
        invalidate();
    }

    public void setViewAlpha(float a) {
        transform(a, scale, transX, transY);
    }

    public float getViewAlpha() {
        return viewAlpha;
    }

    public float getScale() {
        return scale;
    }

    @Override
    public void draw(Canvas canvas) {
        boolean identity = viewAlpha >= 0.998f && Math.abs(scale - 1f) < 0.0005f
                && Math.abs(transX) < 0.5f && Math.abs(transY) < 0.5f;
        if (identity) {
            super.draw(canvas);
            return;
        }
        if (viewAlpha <= 0.004f) return;

        canvas.save();
        canvas.translate(transX, transY);
        canvas.scale(scale, scale, getWidth() * pivotX, getHeight() * pivotY);
        if (viewAlpha < 0.998f) {
            layerRect.set(0, 0, getWidth(), getHeight());
            int sc = canvas.saveLayerAlpha(layerRect, (int) (viewAlpha * 255f), SAVE_FLAGS);
            super.draw(canvas);
            canvas.restoreToCount(sc);
        } else {
            super.draw(canvas);
        }
        canvas.restore();
    }
}
