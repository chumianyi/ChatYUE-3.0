package com.chatyue.app.core;

/**
 * 低版本自绘控件的透明度协议。
 *
 * Android 1.5 的 View 没有 setAlpha，只有 protected onSetAlpha(int)（由 Drawable 体系驱动），
 * 因此凡是需要淡入淡出的自绘控件，统一实现本接口，由 {@link Ui#setAlpha} 分发。
 */
public interface AlphaView {

    void setViewAlpha(float a);

    float getViewAlpha();
}
