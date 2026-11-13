package com.chatyue.app.ui.m3;

import android.content.Context;

import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.RoundBox;

/** M3 卡片：24–28dp 圆角、1dp 描边、几乎不用重阴影。 */
public class M3Card extends RoundBox {

    public M3Card(Context c) {
        this(c, M3.SURFACE, M3.OUTLINE_VARIANT);
    }

    public M3Card(Context c, int fill, int outline) {
        super(c);
        setFill(fill);
        setRadius(26);
        if (outline != 0) setStroke(outline, 1f);
        setPadding(Ui.dp(0), Ui.dp(0), Ui.dp(0), Ui.dp(0));
    }

    public M3Card pad(int l, int t, int r, int b) {
        setPadding(Ui.dp(l), Ui.dp(t), Ui.dp(r), Ui.dp(b));
        return this;
    }
}
