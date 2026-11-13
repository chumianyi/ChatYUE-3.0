package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 统一圆角容器：卡片、气泡、输入条、面板…… 全部基于它。
 * 阴影不用 setShadowLayer（各版本表现不一致），改为多层半透明圆角矩形叠出来。
 */
public class RoundBox extends AlphaFrame {

    private int fill = Palette.CARD;
    private int strokeColor = 0;
    private float strokeW = 1f;
    private float radiusDp;
    private float shadowDp = 0f;
    private int shadowColor = Palette.SHADOW;
    private float tl = -1, tr = -1, br = -1, bl = -1;

    protected final RectF rect = new RectF();
    protected final Path path = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RoundBox(Context c) {
        super(c);
        setWillNotDraw(false);
        radiusDp = 18;
    }

    public RoundBox setFill(int color) {
        fill = color;
        invalidate();
        return this;
    }

    public int fillColor() {
        return fill;
    }

    public RoundBox setRadius(float dp) {
        radiusDp = dp;
        invalidate();
        return this;
    }

    public RoundBox setStroke(int color, float widthDp) {
        strokeColor = color;
        strokeW = widthDp;
        invalidate();
        return this;
    }

    public RoundBox setShadow(float dp) {
        shadowDp = dp;
        invalidate();
        return this;
    }

    public RoundBox setShadow(int color, float dp) {
        shadowColor = color;
        shadowDp = dp;
        invalidate();
        return this;
    }

    public RoundBox corners(float TL, float TR, float BR, float BL) {
        tl = Ui.dpf(TL);
        tr = Ui.dpf(TR);
        br = Ui.dpf(BR);
        bl = Ui.dpf(BL);
        invalidate();
        return this;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rect.set(0, 0, w, h);
    }

    protected void buildPath() {
        path.reset();
        float r = Ui.dpf(radiusDp);
        if (tl < 0) {
            path.addRoundRect(rect, r, r, Path.Direction.CW);
        } else {
            path.addRoundRect(rect, new float[]{tl, tl, tr, tr, br, br, bl, bl}, Path.Direction.CW);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        buildPath();
        if (shadowDp > 0f) {
            float spread = Ui.dpf(shadowDp);
            for (int i = 4; i >= 1; i--) {
                float grow = spread * i / 4f;
                shadowPaint.setColor(Ui.alpha(shadowColor,
                        Math.max(2, ((shadowColor >>> 24) & 0xff) / (i + 1))));
                RectF rr = new RectF(rect.left - grow * 0.3f, rect.top + grow * 0.7f,
                        rect.right + grow * 0.3f, rect.bottom + grow);
                float r = Ui.dpf(radiusDp) + grow * 0.6f;
                canvas.drawRoundRect(rr, r, r, shadowPaint);
            }
        }
        paint.setColor(fill);
        canvas.drawPath(path, paint);
        if (strokeColor != 0) {
            stroke.setColor(strokeColor);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Ui.dpf(strokeW));
            canvas.drawPath(path, stroke);
        }
        super.onDraw(canvas);
    }
}
