package com.chatyue.app.ui.overlay;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.AlphaFrame;
import com.chatyue.app.core.Tween;

/**
 * 浮层宿主：一层遮罩 + 一层面板。
 * 所有弹窗、菜单、选择器都挂到这里，杜绝系统 AlertDialog / PopupWindow 的默认外观。
 */
public class OverlayHost extends FrameLayout {

    private final AlphaFrame scrim;
    private final AlphaFrame layer;
    private Runnable scrimAction;
    private Runnable closeAction;
    private boolean open;
    private Tween fade;
    private int generation;

    public OverlayHost(Context c) {
        super(c);
        setVisibility(GONE);
        scrim = new AlphaFrame(c);
        scrim.setBackgroundColor(Palette.SCRIM);
        scrim.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (scrimAction != null) scrimAction.run();
            }
        });
        addView(scrim, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        layer = new AlphaFrame(c);
        addView(layer, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }

    public FrameLayout layer() {
        return layer;
    }

    public boolean isOpen() {
        return open;
    }

    public void setCloseAction(Runnable r) {
        closeAction = r;
    }

    public void close() {
        if (!open) return;
        if (closeAction != null) {
            Runnable r = closeAction;
            closeAction = null;
            r.run();
        } else {
            hide();
        }
    }

    public void show(Runnable onScrim) {
        generation++;
        scrimAction = onScrim;
        open = true;
        setVisibility(VISIBLE);
        layer.removeAllViews();
        Ui.setAlpha(scrim, 0);
        Ui.setAlpha(layer, 0);
        if (fade != null) fade.cancel();
        fade = Tween.of(0, 255, 100, new Tween.Listener() {
            public void onUpdate(float v) {
                Ui.setAlpha(scrim, v * 0.62f);
                Ui.setAlpha(layer, v);
            }

            public void onEnd() {
            }
        });
        fade.start();
    }

    public void hide() {
        open = false;
        scrimAction = null;
        closeAction = null;
        final int gen = generation;
        if (fade != null) fade.cancel();
        fade = Tween.of(255, 0, 120, new Tween.Listener() {
            public void onUpdate(float v) {
                // 若期间已经打开了新浮层，这次淡出不再影响它
                if (gen != generation) return;
                Ui.setAlpha(scrim, v * 0.62f);
                Ui.setAlpha(layer, v);
            }

            public void onEnd() {
                if (gen != generation) return;
                setVisibility(GONE);
                layer.removeAllViews();
                Ui.setAlpha(layer, 1f);
                Ui.setAlpha(scrim, 1f);
            }
        });
        fade.start();
    }
}
