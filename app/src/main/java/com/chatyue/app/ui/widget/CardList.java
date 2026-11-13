package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.widget.LinearLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 白色圆角卡片容器：内部竖排，自动插入分隔线，并把子树裁剪到圆角内，
 * 避免行按压时的高亮矩形把卡片四个圆角切掉。
 */
public class CardList extends RoundBox {

    private final LinearLayout column;

    public CardList(Context c) {
        super(c);
        setFill(Palette.CARD);
        setRadius(20);
        column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        addView(column, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.FILL_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public LinearLayout column() {
        return column;
    }

    public CardList addRow(MenuRow row) {
        if (column.getChildCount() > 0) addDivider();
        column.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return this;
    }

    public CardList add(View v) {
        column.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return this;
    }

    public CardList add(View v, LinearLayout.LayoutParams lp) {
        column.addView(v, lp);
        return this;
    }

    public CardList addDivider() {
        Divider d = new Divider(getContext());
        d.inset(56);
        column.addView(d, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Math.max(1, Ui.dp(1))));
        return this;
    }

    /** 内边距（用于表单类卡片）。 */
    public CardList padding(float h, float v) {
        column.setPadding(Ui.dp(h), Ui.dp(v), Ui.dp(h), Ui.dp(v));
        return this;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        buildPath();
        int sc = canvas.save();
        canvas.clipPath(path);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(sc);
    }
}
