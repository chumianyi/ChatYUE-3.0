package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;

import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/**
 * Material 3 设计令牌。经典主题不是把默认界面换一层颜色，
 * 而是用这一整套「surface / primary container / outline」语义色重画页面。
 */
public final class M3 {

    /** 页面底色，比纯白暖一点，接近截图里的粉白。 */
    public static final int BG = 0xFFFDF7F8;
    public static final int SURFACE = 0xFFFFFFFF;
    public static final int SURFACE_LOW = 0xFFFBEEF0;
    public static final int SURFACE_CONTAINER = 0xFFF7E6E8;
    public static final int SHEET = 0xFFFCEFF1;

    public static final int PRIMARY = 0xFFB3261E;
    public static final int ON_PRIMARY = 0xFFFFFFFF;
    public static final int PRIMARY_CONTAINER = 0xFFF9D8DA;
    public static final int ON_PRIMARY_CONTAINER = 0xFF410006;
    public static final int PRIMARY_DEEP = 0xFF8C1D18;

    public static final int ON_SURFACE = 0xFF211A1B;
    public static final int ON_SURFACE_2 = 0xFF5B4042;
    public static final int ON_SURFACE_3 = 0xFF8C7072;
    public static final int OUTLINE = 0xFF8C7072;
    public static final int OUTLINE_VARIANT = 0xFFE6CDD0;

    public static final int ERROR = 0xFFBA1A1A;
    public static final int ERROR_CONTAINER = 0xFFF9DEDC;
    public static final int SUCCESS = 0xFF3F6B54;
    public static final int BADGE = 0xFFBE2A2A;
    public static final int SCRIM = 0x66150D0F;

    private M3() {
    }

    public static TextPaint text(int color, float sp, boolean bold) {
        return Ui.textPaint(color, sp, bold);
    }

    public static Paint fill(int color) {
        return Ui.fillPaint(color);
    }

    public static Paint stroke(int color, float dp) {
        return Ui.strokePaint(color, dp);
    }

    public static View space(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new ViewGroup.LayoutParams(1, Ui.dp(dp)));
        return v;
    }

    /**
     * 页面内容的分段入场：每块内容淡入并向上抬 8dp，间隔 26ms。
     * 这是「细节」而不是炫技，滑动页面时不会抢注意力。
     */
    public static void reveal(final ViewGroup root) {
        if (root == null) return;
        root.post(new Runnable() {
            public void run() {
                for (int i = 0; i < root.getChildCount(); i++) {
                    final View v = root.getChildAt(i);
                    if (v.getVisibility() != View.VISIBLE) continue;
                    Ui.setAlpha(v, 0f);
                    final int from = Ui.dp(9);
                    v.offsetTopAndBottom(from);
                    final int delay = Math.min(220, i * 28);
                    Tween t = Tween.of(0, 1, 230, new Tween.Listener() {
                        public void onUpdate(float p) {
                            Ui.setAlpha(v, p);
                            v.offsetTopAndBottom((int) (from * (1f - p)));
                        }

                        public void onEnd() {
                            Ui.setAlpha(v, 1f);
                            v.offsetTopAndBottom(0);
                        }
                    }).delay(delay);
                    t.ease(Tween.DECEL).start();
                }
            }
        });
    }

    public static int pressed(int base, float press) {
        return Ui.mix(base, 0xFF000000, 0.055f * Ui.clamp(press, 0f, 1f));
    }
}
