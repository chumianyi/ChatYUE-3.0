package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/**
 * M3 列表行。两种形态：
 * - plain：底部面板里的「图标 + 文字」，无底座，和截图一致；
 * - tonal：设置页里的「浅色圆底图标 + 标题 + 支持文字」。
 */
public class M3Row extends ViewGroup {

    private int icon;
    private String title = "";
    private String desc = "";
    private String value = "";
    private boolean plain;
    private boolean chevron;
    private boolean checked;
    private boolean danger;
    private boolean divider;
    private boolean dividerInset = true;
    private int iconBg = M3.PRIMARY_CONTAINER;
    private int iconFg = M3.ON_PRIMARY_CONTAINER;
    private View trailing;
    private float press;
    private Tween pressTween;

    private final TextPaint titlePaint;
    private final TextPaint descPaint = Ui.textPaint(M3.ON_SURFACE_3, 11.8f, false);
    private final TextPaint valuePaint = Ui.textPaint(M3.ON_SURFACE_2, 12.4f, false);
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = Ui.fillPaint(M3.OUTLINE_VARIANT);
    private final RectF rect = new RectF();

    public M3Row(Context c, boolean plain) {
        super(c);
        this.plain = plain;
        titlePaint = Ui.textPaint(M3.ON_SURFACE, plain ? 16f : 15.2f, false);
        setWillNotDraw(false);
        setClickable(true);
    }

    public M3Row icon(int id) {
        icon = id;
        invalidate();
        return this;
    }

    public M3Row icon(int id, int bgColor, int fgColor) {
        icon = id;
        iconBg = bgColor;
        iconFg = fgColor;
        invalidate();
        return this;
    }

    public M3Row title(String t) {
        title = t == null ? "" : t;
        requestLayout();
        return this;
    }

    public M3Row desc(String d) {
        desc = d == null ? "" : d;
        requestLayout();
        return this;
    }

    public M3Row value(String v) {
        value = v == null ? "" : v;
        requestLayout();
        return this;
    }

    public M3Row chevron(boolean c) {
        chevron = c;
        return this;
    }

    public M3Row checked(boolean c) {
        checked = c;
        return this;
    }

    public M3Row danger(boolean d) {
        danger = d;
        titlePaint.setColor(d ? M3.ERROR : M3.ON_SURFACE);
        iconFg = d ? M3.ERROR : M3.ON_PRIMARY_CONTAINER;
        return this;
    }

    public M3Row divider(boolean d) {
        divider = d;
        return this;
    }

    public M3Row dividerInset(boolean inset) {
        dividerInset = inset;
        return this;
    }

    public M3Row trailing(View v) {
        trailing = v;
        if (v != null) addView(v);
        requestLayout();
        return this;
    }

    public View getTrailing() {
        return trailing;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Ui.dp(desc.length() > 0 ? 74 : (plain ? 58 : 66));
        if (trailing != null) {
            trailing.measure(MeasureSpec.makeMeasureSpec(Ui.dp(100), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (trailing != null) {
            int tw = trailing.getMeasuredWidth();
            int th = trailing.getMeasuredHeight();
            int right = getWidth() - Ui.dp(18);
            int top = (getHeight() - th) / 2;
            trailing.layout(right - tw, top, right, top + th);
        }
    }

    private int leadingTextX() {
        return Ui.dp(plain ? 56 : 68);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        if (press > 0.01f) {
            bg.setColor(Ui.alpha(0xFF000000, (int) (14 * press)));
            canvas.drawRoundRect(rect, Ui.dpf(22), Ui.dpf(22), bg);
        }

        float iconCx;
        float iconCy = getHeight() / 2f;
        if (desc.length() > 0) iconCy = Ui.dpf(30);
        if (plain) {
            iconCx = Ui.dpf(20);
            Icons.draw(canvas, icon, iconCx, iconCy, Ui.dpf(24), iconFg, 1.8f);
        } else {
            iconCx = Ui.dpf(38);
            int r = Ui.dp(20);
            bg.setColor(iconBg);
            canvas.drawCircle(iconCx, iconCy, r, bg);
            Icons.draw(canvas, icon, iconCx, iconCy, Ui.dpf(20), iconFg, 1.7f);
        }

        int textX = leadingTextX();
        int rightLimit = getWidth() - Ui.dp(18);
        if (trailing != null) rightLimit = getWidth() - Ui.dp(18) - trailing.getMeasuredWidth() - Ui.dp(8);
        else if (chevron || checked) rightLimit = getWidth() - Ui.dp(40);
        if (value.length() > 0) rightLimit -= (int) valuePaint.measureText(value) + Ui.dp(10);

        int ty;
        if (desc.length() > 0) {
            ty = Ui.dp(22);
            canvas.drawText(Ui.ellipsize(titlePaint, title, rightLimit - textX), textX,
                    Ui.baselineCentered(titlePaint, ty), titlePaint);
            canvas.drawText(Ui.ellipsize(descPaint, desc, rightLimit - textX), textX,
                    Ui.baselineCentered(descPaint, Ui.dpf(48)), descPaint);
        } else {
            ty = getHeight() / 2;
            canvas.drawText(Ui.ellipsize(titlePaint, title, rightLimit - textX), textX,
                    Ui.baselineCentered(titlePaint, ty), titlePaint);
        }

        int right = getWidth() - Ui.dp(18);
        if (value.length() > 0) {
            canvas.drawText(value, right - valuePaint.measureText(value), Ui.baselineCentered(valuePaint, getHeight() / 2f), valuePaint);
        }
        if (checked) {
            Icons.draw(canvas, Icons.CHECK, right - Ui.dpf(9), getHeight() / 2f, Ui.dpf(19), M3.PRIMARY, 1.8f);
        } else if (chevron) {
            Icons.draw(canvas, Icons.CHEVRON_RIGHT, right - Ui.dpf(6), getHeight() / 2f, Ui.dpf(18), M3.ON_SURFACE_3, 1.6f);
        }

        if (divider) {
            float left = dividerInset ? Ui.dpf(56) : 0;
            canvas.drawRect(left, getHeight() - Ui.dp(1), getWidth(), getHeight(), line);
        }
        super.onDraw(canvas);
    }

    private void animatePress(final boolean down) {
        if (pressTween != null) pressTween.cancel();
        pressTween = Tween.of(press, down ? 1f : 0f, down ? 90 : 170, new Tween.Listener() {
            public void onUpdate(float v) {
                press = v;
                invalidate();
            }

            public void onEnd() {
            }
        });
        pressTween.ease(down ? Tween.DECEL : Tween.STANDARD).start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animatePress(true);
                return true;
            case MotionEvent.ACTION_UP:
                animatePress(false);
                if (e.getX() >= 0 && e.getX() <= getWidth() && e.getY() >= 0 && e.getY() <= getHeight()) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                animatePress(false);
                return true;
        }
        return super.onTouchEvent(e);
    }
}
