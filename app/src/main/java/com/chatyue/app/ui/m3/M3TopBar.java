package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;

/**
 * M3 顶栏：左侧圆形返回/头像，标题与副标题左对齐，右侧动作。
 * 和默认主题的“中间胶囊标题”完全不同，留白更大。
 */
public class M3TopBar extends ViewGroup {

    public final IconButton left;
    public final IconButton right;
    public final Label title;
    public final Label subtitle;
    private final float density;
    private final Paint line = Ui.fillPaint(M3.OUTLINE_VARIANT);
    private final RectF rect = new RectF();
    private boolean hasLeft;
    private boolean hasRight;
    private boolean showLine;

    public M3TopBar(Context c, String titleText, String subText, int leftIcon, int rightIcon) {
        super(c);
        setWillNotDraw(false);
        density = Ui.density();
        hasLeft = leftIcon != 0;
        hasRight = rightIcon != 0;

        left = new IconButton(c, leftIcon == 0 ? Icons.BACK : leftIcon, 0,
                leftIcon == Icons.USER ? M3.ON_PRIMARY_CONTAINER : M3.ON_SURFACE, 40, 20);
        if (leftIcon == Icons.USER) {
            left.setColors(M3.PRIMARY_CONTAINER, M3.ON_PRIMARY_CONTAINER);
        }
        addView(left);

        title = new Label(c, titleText, 20.5f, M3.ON_SURFACE, true);
        addView(title);
        subtitle = new Label(c, subText == null ? "" : subText, 11.6f, M3.ON_SURFACE_3, false);
        addView(subtitle);

        if (hasRight) {
            right = new IconButton(c, rightIcon, 0, M3.ON_SURFACE_2, 40, 19);
            addView(right);
        } else {
            right = null;
        }
    }

    public M3TopBar line(boolean b) {
        showLine = b;
        invalidate();
        return this;
    }

    public void setTitle(String s) {
        title.setText(s);
        requestLayout();
    }

    public void setSubtitle(String s) {
        subtitle.setText(s);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Ui.dp(70);
        if (hasLeft) {
            left.measure(MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY));
        }
        if (hasRight) {
            right.measure(MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY));
        }
        int leftPad = hasLeft ? 60 : 20;
        int rightPad = hasRight ? 60 : 20;
        int tw = Math.max(Ui.dp(80), w - Ui.dp(leftPad + rightPad));
        title.measure(MeasureSpec.makeMeasureSpec(tw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(30), MeasureSpec.EXACTLY));
        subtitle.measure(MeasureSpec.makeMeasureSpec(tw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(18), MeasureSpec.EXACTLY));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int h = getHeight();
        if (hasLeft) {
            int top = (h - Ui.dp(40)) / 2;
            left.layout(Ui.dp(12), top, Ui.dp(52), top + Ui.dp(40));
        }
        if (hasRight) {
            int top = (h - Ui.dp(40)) / 2;
            right.layout(getWidth() - Ui.dp(52), top, getWidth() - Ui.dp(12), top + Ui.dp(40));
        }
        int tx = Ui.dp(hasLeft ? 60 : 20);
        int tw = getWidth() - tx - Ui.dp(hasRight ? 60 : 20);
        int contentH = Ui.dp(30) + (subtitle.getText().length() > 0 ? Ui.dp(2) : 0) + Ui.dp(18);
        int top = (h - contentH) / 2;
        title.layout(tx, top, tx + tw, top + Ui.dp(30));
        subtitle.layout(tx, top + Ui.dp(30) + Ui.dp(2), tx + tw,
                top + Ui.dp(30) + Ui.dp(2) + Ui.dp(18));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (showLine) {
            rect.set(Ui.dpf(20), getHeight() - Ui.dpf(1), getWidth() - Ui.dpf(20), getHeight());
            canvas.drawRect(rect, line);
        }
        super.onDraw(canvas);
    }
}
