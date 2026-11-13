package com.chatyue.app.ui;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.widget.FrameLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.AlphaView;
import com.chatyue.app.core.Palette;

/** 页面基类。页面切换由 MainActivity 驱动，这里只负责把变换画出来。 */
public abstract class Page extends FrameLayout implements AlphaView {

    private static final int SAVE_FLAGS = 31;

    protected final MainActivity act;

    private float pageAlpha = 1f;
    private float pageScale = 1f;
    private int pageOffsetY;
    private final RectF layerRect = new RectF();

    public Page(MainActivity a) {
        super(a);
        act = a;
        setBackgroundColor(Palette.BG);
        setWillNotDraw(false);
    }

    /** 返回 true 表示页面自己消费了返回键。 */
    public boolean onBack() {
        return false;
    }

    public void onPageShown() {
    }

    public void onPageHidden() {
    }

    public void onDataChanged() {
    }

    /** 页面级转场：透明度、缩放、纵向位移。 */
    public void setPageTransform(float alpha, float scale, int offsetY) {
        float a = alpha < 0f ? 0f : (alpha > 1f ? 1f : alpha);
        if (Math.abs(a - pageAlpha) < 0.001f && Math.abs(scale - pageScale) < 0.0005f
                && offsetY == pageOffsetY) {
            return;
        }
        pageAlpha = a;
        pageScale = scale;
        pageOffsetY = offsetY;
        invalidate();
    }

    public void setViewAlpha(float a) {
        setPageTransform(a, pageScale, pageOffsetY);
    }

    public float getViewAlpha() {
        return pageAlpha;
    }

    @Override
    public void draw(Canvas canvas) {
        boolean identity = pageAlpha >= 0.998f && Math.abs(pageScale - 1f) < 0.0005f
                && pageOffsetY == 0;
        if (identity) {
            super.draw(canvas);
            return;
        }
        if (pageAlpha <= 0.004f) return;

        canvas.save();
        canvas.translate(0, pageOffsetY);
        canvas.scale(pageScale, pageScale, getWidth() / 2f, getHeight() / 2f);
        if (pageAlpha < 0.998f) {
            layerRect.set(0, 0, getWidth(), getHeight());
            int sc = canvas.saveLayerAlpha(layerRect, (int) (pageAlpha * 255f), SAVE_FLAGS);
            super.draw(canvas);
            canvas.restoreToCount(sc);
        } else {
            super.draw(canvas);
        }
        canvas.restore();
    }
}
