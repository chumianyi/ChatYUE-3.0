package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.widget.FrameLayout;

import com.chatyue.app.core.AlphaView;

/**
 * 支持整体透明度（含子视图）的容器。
 *
 * 不能用 View.setAlpha —— API 3 上不存在；这里用 API 1 就有的
 * Canvas.saveLayerAlpha(RectF, int, int) 把整棵子树先画进图层再加权合成，
 * 语义与后来的 setAlpha 一致（子视图一起变淡）。
 */
public class AlphaFrame extends FrameLayout implements AlphaView {

    /**
     * Canvas.ALL_SAVE_FLAGS。
     *
     * 传 0 在硬件加速画布上会抛 "Invalid Layer Save Flag - only ALL_SAVE_FLAGS is allowed"，
     * 这正是 2.0.1 五个入口闪退的根因。API 1 里这个常量叫 ALL_SAVE_FLAG，值同为 31，
     * 为了能对着 1.5 的 android.jar 编译，这里直接写字面量。
     */
    private static final int SAVE_FLAGS = 31;

    private float viewAlpha = 1f;
    private final RectF layerRect = new RectF();

    public AlphaFrame(Context c) {
        super(c);
        setWillNotDraw(false);
    }

    public void setViewAlpha(float a) {
        float v = a < 0f ? 0f : (a > 1f ? 1f : a);
        if (Math.abs(v - viewAlpha) > 0.001f) {
            viewAlpha = v;
            invalidate();
        }
    }

    public float getViewAlpha() {
        return viewAlpha;
    }

    @Override
    public void draw(Canvas canvas) {
        if (viewAlpha >= 0.998f) {
            super.draw(canvas);
            return;
        }
        // 全透明时连子树都不用画
        if (viewAlpha <= 0.004f) return;
        // 老版本 Skia 对 null 边界支持不稳定，显式给出自身矩形
        layerRect.set(0, 0, getWidth(), getHeight());
        int sc = canvas.saveLayerAlpha(layerRect, (int) (viewAlpha * 255f), SAVE_FLAGS);
        super.draw(canvas);
        canvas.restoreToCount(sc);
    }
}
