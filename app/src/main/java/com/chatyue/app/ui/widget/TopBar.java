package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.ViewGroup;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 顶栏：左侧圆形返回键 + 中间白色胶囊标题（两行）+ 右侧圆形更多键。
 * 整条栏都是自绘的，没有 ActionBar / Toolbar 参与。
 */
public class TopBar extends ViewGroup {

    public final IconButton left;
    public final IconButton right;
    public final Label title;
    public final Label subtitle;
    private final Paint pillPaint = Ui.fillPaint(0xFFFFFFFF);
    private final RectF pillRect = new RectF();
    private boolean hasRight = true;
    private int pillW;

    public TopBar(Context c, String titleText, String subText, int rightIcon) {
        super(c);
        setWillNotDraw(false);
        left = new IconButton(c, Icons.BACK, Palette.PINK_SOFT, Palette.INK, 40, 19);
        addView(left);
        title = new Label(c, titleText, 15.4f, Palette.INK, true);
        title.setAlign(Label.CENTER);
        addView(title);
        subtitle = new Label(c, subText == null ? "" : subText, 10.6f, Palette.INK_3, false);
        subtitle.setAlign(Label.CENTER);
        addView(subtitle);
        if (rightIcon != 0) {
            right = new IconButton(c, rightIcon, Palette.PINK_SOFT, Palette.INK, 40, 19);
            addView(right);
        } else {
            right = null;
            hasRight = false;
        }
    }

    public void setSubtitle(String s) {
        subtitle.setText(s);
        requestLayout();
    }

    public void setTitle(String s) {
        title.setText(s);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Ui.dp(66);
        left.measure(MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY));
        if (right != null) {
            right.measure(MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY));
        }
        int sideSpace = Ui.dp(16) + Ui.dp(40) + Ui.dp(10);
        int maxPill = Math.max(Ui.dp(120), w - sideSpace * 2);
        int tw = (int) Math.ceil(title.paint().measureText(title.getText()));
        int sw = (int) Math.ceil(subtitle.paint().measureText(subtitle.getText()));
        pillW = Math.min(maxPill, Math.max(tw, sw) + Ui.dp(52));
        int textW = Math.max(Ui.dp(40), pillW - Ui.dp(24));
        title.measure(MeasureSpec.makeMeasureSpec(textW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(20), MeasureSpec.EXACTLY));
        subtitle.measure(MeasureSpec.makeMeasureSpec(textW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(15), MeasureSpec.EXACTLY));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getWidth();
        int h = getHeight();
        int size = Ui.dp(40);
        int leftTop = (h - size) / 2;
        left.layout(Ui.dp(16), leftTop, Ui.dp(16) + size, leftTop + size);
        if (right != null) {
            right.layout(w - Ui.dp(16) - size, leftTop, w - Ui.dp(16), leftTop + size);
        }
        int pw = pillW > 0 ? pillW : Math.max(title.getMeasuredWidth(), subtitle.getMeasuredWidth()) + Ui.dp(52);
        int pillH = Ui.dp(46);
        int pillTop = (h - pillH) / 2;
        int pillLeft = (w - pw) / 2;
        pillRect.set(pillLeft, pillTop, pillLeft + pw, pillTop + pillH);

        int contentH = title.getMeasuredHeight() + Ui.dp(1) + subtitle.getMeasuredHeight();
        int top = pillTop + (pillH - contentH) / 2;
        int tw = pw - Ui.dp(24);
        int tx = pillLeft + Ui.dp(12);
        title.layout(tx, top, tx + tw, top + title.getMeasuredHeight());
        int sy = top + title.getMeasuredHeight() + Ui.dp(1);
        subtitle.layout(tx, sy, tx + tw, sy + subtitle.getMeasuredHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(pillRect, pillRect.height() / 2f, pillRect.height() / 2f, pillPaint);
        super.onDraw(canvas);
    }
}
