package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 通用列表行：设置项、模型提供方、底部菜单项都用它。
 * 卡片形态由 position(first/last) 决定圆角，用 Path 手工拼四个角。
 */
public class MenuRow extends ViewGroup {

    public static final int STYLE_CARD = 0;
    public static final int STYLE_PLAIN = 1;

    private final int style;
    private int icon;
    private int iconBg = Palette.PINK_SOFT;
    private int iconFg = Palette.PINK_DEEP;
    private float iconSizeDp = 34;
    private String title = "";
    private String desc = "";
    private String value = "";
    private boolean chevron = true;
    private boolean danger;
    private boolean checked;
    private boolean first = true;
    private boolean last = true;
    private boolean showDivider = true;
    private boolean pressed;
    private View trailing;
    private int trailingW;

    private final TextPaint titlePaint = Ui.textPaint(Palette.INK, 15.2f, false);
    private final TextPaint descPaint = Ui.textPaint(Palette.INK_3, 11.6f, false);
    private final TextPaint valuePaint = Ui.textPaint(Palette.INK_3, 12.6f, false);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = Ui.fillPaint(Palette.LINE_2);
    private final Paint iconBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public MenuRow(Context c, int style) {
        super(c);
        this.style = style;
        setWillNotDraw(false);
        setClickable(true);
    }

    public MenuRow icon(int id, int bg, int fg) {
        icon = id;
        iconBg = bg;
        iconFg = fg;
        return this;
    }

    /** 用文字（如「主」「C」）作圆形头像的图标。 */
    public MenuRow letter(String text, int bg, int fg) {
        icon = -1;
        letter = text;
        iconBg = bg;
        iconFg = fg;
        return this;
    }

    private String letter;

    public MenuRow title(String t) {
        title = t == null ? "" : t;
        requestLayout();
        return this;
    }

    public MenuRow desc(String d) {
        desc = d == null ? "" : d;
        requestLayout();
        return this;
    }

    public MenuRow value(String v) {
        value = v == null ? "" : v;
        requestLayout();
        return this;
    }

    /** 行尾挂一个自绘控件（开关、步进器）。 */
    public MenuRow trailing(View v) {
        trailing = v;
        addView(v);
        requestLayout();
        return this;
    }

    public MenuRow chevron(boolean c) {
        chevron = c;
        return this;
    }

    public MenuRow danger(boolean d) {
        danger = d;
        titlePaint.setColor(d ? Palette.RED : Palette.INK);
        return this;
    }

    public MenuRow checked(boolean c) {
        checked = c;
        return this;
    }

    public MenuRow position(boolean f, boolean l) {
        first = f;
        last = l;
        return this;
    }

    public MenuRow divider(boolean d) {
        showDivider = d;
        return this;
    }

    public void setTitle(String t) {
        title(t);
        invalidate();
    }

    public void setDesc(String d) {
        desc(d);
        invalidate();
    }

    public void setValue(String v) {
        value(v);
        invalidate();
    }

    public String getTitle() {
        return title;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w = Ui.screenWidth() - Ui.dp(32);
        }
        int h = desc.length() > 0 ? Ui.dp(72) : Ui.dp(60);
        if (style == STYLE_PLAIN) h = desc.length() > 0 ? Ui.dp(62) : Ui.dp(52);
        trailingW = 0;
        if (trailing != null) {
            trailing.measure(MeasureSpec.makeMeasureSpec(Ui.dp(124), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
            trailingW = trailing.getMeasuredWidth();
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (trailing != null) {
            int tw = trailing.getMeasuredWidth();
            int th = trailing.getMeasuredHeight();
            int right = getWidth() - Ui.dp(16);
            int top = (getHeight() - th) / 2;
            trailing.layout(right - tw, top, right, top + th);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        rect.set(0, 0, w, h);
        if (style == STYLE_CARD) {
            float r = Ui.dpf(20);
            float tl = first ? r : 0;
            float tr = first ? r : 0;
            float br = last ? r : 0;
            float bl = last ? r : 0;
            path.reset();
            path.addRoundRect(rect, new float[]{tl, tl, tr, tr, br, br, bl, bl}, Path.Direction.CW);
            bgPaint.setColor(pressed ? Palette.BG_DEEP : Palette.CARD);
            canvas.drawPath(path, bgPaint);
            if (!last && showDivider) {
                canvas.drawRect(Ui.dpf(56), h - Math.max(1, Ui.dpf(0.8f)), w - Ui.dp(12), h, dividerPaint);
            }
        } else {
            if (pressed) {
                bgPaint.setColor(Palette.BG_DEEP);
                canvas.drawRect(rect, bgPaint);
            }
        }

        int left = Ui.dp(16);
        if (style == STYLE_PLAIN) left = Ui.dp(8);
        int iconSize = Ui.dp(iconSizeDp);
        int iconLeft = left;
        int titleLeft = left;

        if (icon != 0) {
            float cy = h / 2f;
            iconBgPaint.setColor(iconBg);
            canvas.drawCircle(iconLeft + iconSize / 2f, cy, iconSize / 2f, iconBgPaint);
            if (icon > 0) {
                Icons.draw(canvas, icon, iconLeft + iconSize / 2f, cy, Ui.dpf(18), iconFg, 1.8f);
            } else if (letter != null) {
                TextPaint lp = Ui.textPaint(iconFg, 14f, true);
                lp.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(letter, iconLeft + iconSize / 2f,
                        Ui.baselineCentered(lp, cy), lp);
            }
            titleLeft = iconLeft + iconSize + Ui.dp(12);
        } else {
            titleLeft = left + Ui.dp(4);
        }

        float rightLimit = w - Ui.dp(14);
        if (chevron) rightLimit -= Ui.dp(20);
        if (value.length() > 0) rightLimit -= valuePaint.measureText(value) + Ui.dp(8);
        if (checked) rightLimit -= Ui.dp(22);
        if (trailingW > 0) rightLimit -= trailingW + Ui.dp(10);

        if (desc.length() > 0) {
            float ty = h / 2f - Ui.dpf(9);
            canvas.drawText(Ui.ellipsize(titlePaint, title, rightLimit - titleLeft),
                    titleLeft, Ui.baselineCentered(titlePaint, ty), titlePaint);
            float dy = h / 2f + Ui.dpf(11);
            canvas.drawText(Ui.ellipsize(descPaint, desc, rightLimit - titleLeft),
                    titleLeft, Ui.baselineCentered(descPaint, dy), descPaint);
        } else {
            canvas.drawText(Ui.ellipsize(titlePaint, title, rightLimit - titleLeft),
                    titleLeft, Ui.baselineCentered(titlePaint, h / 2f), titlePaint);
        }

        float rx = w - Ui.dp(14);
        if (chevron) {
            Icons.draw(canvas, Icons.CHEVRON_RIGHT, rx - Ui.dpf(6), h / 2f, Ui.dpf(14),
                    Palette.INK_4, 1.8f);
        }
        if (checked) {
            Icons.draw(canvas, Icons.CHECK, rx - Ui.dpf(8), h / 2f, Ui.dpf(16), Palette.PINK, 2f);
        }
        if (value.length() > 0) {
            float vw = valuePaint.measureText(value);
            canvas.drawText(value, rx - (chevron ? Ui.dp(18) : 0) - vw, Ui.baselineCentered(valuePaint, h / 2f), valuePaint);
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPress(true);
                return true;
            case MotionEvent.ACTION_UP:
                setPress(false);
                if (e.getX() >= 0 && e.getX() <= getWidth() && e.getY() >= 0 && e.getY() <= getHeight()) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPress(false);
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void setPress(boolean down) {
        pressed = down;
        invalidate();
    }
}
