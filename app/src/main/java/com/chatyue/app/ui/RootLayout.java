package com.chatyue.app.ui;

import android.content.Context;
import android.widget.FrameLayout;

import com.chatyue.app.ui.overlay.OverlayHost;

/** 根容器：页面层 / 浮层 / 轻提示 / 加载层，层级关系固定，避免浮层被页面裁剪。 */
public class RootLayout extends FrameLayout {

    public final FrameLayout pageHost;
    public final OverlayHost overlay;
    public final FrameLayout toastLayer;
    public final FrameLayout loadingLayer;

    public RootLayout(Context c) {
        super(c);

        pageHost = new FrameLayout(c);
        addView(pageHost, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        toastLayer = new FrameLayout(c);
        toastLayer.setClickable(false);
        addView(toastLayer, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        overlay = new OverlayHost(c);
        addView(overlay, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        loadingLayer = new FrameLayout(c);
        addView(loadingLayer, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }
}
