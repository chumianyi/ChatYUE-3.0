package com.chatyue.app.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.View;

import java.lang.reflect.Method;

/**
 * 尺寸 / 画笔 / 绘制的统一工具。
 *
 * 全部基于 API 1 就已存在的 Canvas / Paint 能力，不依赖任何资源体系与密度适配框架。
 */
public final class Ui {

    private static float sDensity = 1f;
    private static float sScaled = 1f;
    private static int sWidth = 320;
    private static int sHeight = 480;
    private static Method sSetAlpha;

    private Ui() {
    }

    public static void init(Context c) {
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        sDensity = m.density <= 0f ? 1f : m.density;
        sScaled = m.scaledDensity <= 0f ? sDensity : m.scaledDensity;
        sWidth = m.widthPixels;
        sHeight = m.heightPixels;
    }

    public static float density() {
        return sDensity;
    }

    public static int dp(float v) {
        return Math.round(v * sDensity);
    }

    public static float dpf(float v) {
        return v * sDensity;
    }

    public static int screenWidth() {
        return sWidth;
    }

    public static int screenHeight() {
        return sHeight;
    }

    public static TextPaint textPaint(int color, float sizeDp, boolean bold) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(sizeDp * sScaled);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        p.setSubpixelText(true);
        return p;
    }

    public static Paint fillPaint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    public static Paint strokePaint(int color, float widthDp) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(widthDp * sDensity);
        p.setColor(color);
        return p;
    }

    public static void rect(Canvas c, RectF r, float radius, Paint p) {
        c.drawRoundRect(r, radius, radius, p);
    }

    public static Path roundPath(RectF r, float radius) {
        Path path = new Path();
        path.addRoundRect(r, radius, radius, Path.Direction.CW);
        return path;
    }

    /** 支持四个角不同圆角，低版本没有 setCornerRadii，用 Path.arcTo 自行拼装。 */
    public static Path roundPath(RectF r, float tl, float tr, float br, float bl) {
        Path p = new Path();
        float[] radii = new float[]{tl, tl, tr, tr, br, br, bl, bl};
        p.addRoundRect(r, radii, Path.Direction.CW);
        return p;
    }

    /** 把颜色按比例与另一种颜色混合，t=0 取 a，t=1 取 b。 */
    public static int mix(int a, int b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        int aa = (a >>> 24) & 0xff;
        int ar = (a >>> 16) & 0xff;
        int ag = (a >>> 8) & 0xff;
        int ab = a & 0xff;
        int ba = (b >>> 24) & 0xff;
        int br = (b >>> 16) & 0xff;
        int bg = (b >>> 8) & 0xff;
        int bb = b & 0xff;
        int ca = (int) (aa + (ba - aa) * t);
        int cr = (int) (ar + (br - ar) * t);
        int cg = (int) (ag + (bg - ag) * t);
        int cb = (int) (ab + (bb - ab) * t);
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    public static int alpha(int color, int a) {
        return (color & 0x00ffffff) | ((a & 0xff) << 24);
    }

    public static int alphaOf(int color) {
        return (color >>> 24) & 0xff;
    }

    /** 手写软阴影：不同 API 的 setShadowLayer 表现不一致，用多层半透明圆角矩形近似。 */
    public static void softShadow(Canvas c, RectF r, float radius, int color, float spreadDp) {
        int layers = 4;
        float spread = dpf(spreadDp);
        Paint p = fillPaint(0);
        for (int i = layers; i >= 1; i--) {
            float grow = spread * i / layers;
            p.setColor(alpha(color, Math.max(1, alphaOf(color) / (i + 2))));
            RectF rr = new RectF(r.left - grow * 0.35f, r.top + grow * 0.55f,
                    r.right + grow * 0.35f, r.bottom + grow);
            c.drawRoundRect(rr, radius + grow * 0.5f, radius + grow * 0.5f, p);
        }
    }

    public static String ellipsize(Paint p, String text, float maxWidth) {
        if (text == null) return "";
        if (p.measureText(text) <= maxWidth) return text;
        String dots = "...";
        float dotsW = p.measureText(dots);
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (p.measureText(text, 0, mid) + dotsW <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return text.substring(0, Math.max(0, lo)) + dots;
    }

    public static void text(Canvas c, String s, float x, float y, Paint p) {
        c.drawText(s, x, y, p);
    }

    /** 文本基线：让文字在给定矩形内垂直居中。 */
    public static float baseline(Paint p, Rect r) {
        Paint.FontMetricsInt fm = p.getFontMetricsInt();
        return r.centerY() - (fm.ascent + fm.descent) / 2f;
    }

    public static float baselineCentered(Paint p, float centerY) {
        Paint.FontMetricsInt fm = p.getFontMetricsInt();
        return centerY - (fm.ascent + fm.descent) / 2f;
    }

    public static float baselineTop(Paint p, float top) {
        Paint.FontMetricsInt fm = p.getFontMetricsInt();
        return top - fm.ascent;
    }

    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static boolean hit(RectF r, float x, float y) {
        return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
    }

    public static boolean hit(Rect r, float x, float y) {
        return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
    }

    /**
     * 统一的透明度入口。
     *
     * API 3 的 View 没有 setAlpha，直接写 view.setAlpha(...) 在 1.5 上会 NoSuchMethodError；
     * 自绘控件走 {@link AlphaView}，平台控件则反射调用（只有 API 11+ 才存在该方法）。
     */
    public static void setAlpha(View v, float a) {
        if (v == null) return;
        float x = clamp(a, 0f, 1f);
        if (v instanceof AlphaView) {
            ((AlphaView) v).setViewAlpha(x);
            return;
        }
        try {
            if (sSetAlpha == null) {
                sSetAlpha = View.class.getMethod("setAlpha", new Class[]{Float.TYPE});
            }
            sSetAlpha.invoke(v, new Object[]{Float.valueOf(x)});
        } catch (Throwable ignored) {
            // 低版本无此方法：控件保持不透明，不影响功能
        }
    }

    public static float alphaOf(View v) {
        if (v instanceof AlphaView) return ((AlphaView) v).getViewAlpha();
        return 1f;
    }
}
