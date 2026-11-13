package com.chatyue.app.core;

/**
 * 全套配色。整体为「奶油粉 + 极浅灰底 + 纯白卡片」的克制体系。
 *
 * 注意：这里的颜色字段刻意不加 final —— 主题切换时 {@link #apply(int)} 直接改字段，
 * 全应用所有自绘控件下一次 onDraw 就会用上新颜色（final 会在编译期被内联，改不动）。
 */
public final class Palette {

    public static int BG = 0xFFF6F3F5;
    public static int BG_DEEP = 0xFFF0ECEF;
    public static int CARD = 0xFFFFFFFF;
    public static int CARD_SOFT = 0xFFFBFAFB;

    public static int INK = 0xFF1C1B1D;
    public static int INK_2 = 0xFF6E6B72;
    public static int INK_3 = 0xFF9B979F;
    public static int INK_4 = 0xFFBFBAC2;

    public static int PINK = 0xFFEF7FA8;
    public static int PINK_DEEP = 0xFFE2658F;
    public static int PINK_SOFT = 0xFFFBE3EB;
    public static int PINK_TINT = 0xFFFDF2F6;
    public static int PINK_LINE = 0xFFF6D9E4;

    public static int USER_BUBBLE = 0xFFFBDEE8;
    public static int BOT_BUBBLE = 0xFFFFFFFF;

    public static int LINE = 0xFFEDE8EC;
    public static int LINE_2 = 0xFFF3EFF2;

    public static int DARK = 0xFF1F1E21;
    public static int CODE_BG = 0xFFF5F2F5;
    public static int CODE_HEAD = 0xFFEFEBEF;

    public static int GREEN = 0xFF38A169;
    public static int RED = 0xFFE05252;
    public static int BLUE = 0xFF4A7BD8;
    public static int AMBER = 0xFFD99B2B;

    public static int SHADOW = 0x1A2A1B22;
    public static int SCRIM = 0x660F0A0D;

    private Palette() {
    }

    /** 按主题索引重算派生色。 */
    public static void apply(int themeIndex) {
        if (themeIndex == Theme.CLASSIC) {
            // 经典主题是 Material 3 的粉白表面 + 红主色，派生色保持低饱和，
            // 保证旧控件（Toast、Markdown、默认页）也不会突然变成高饱和粉。
            BG = 0xFFFDF7F8;
            BG_DEEP = 0xFFF5E9EB;
            CARD = 0xFFFFFFFF;
            CARD_SOFT = 0xFFFCF4F5;
            INK = 0xFF211A1B;
            INK_2 = 0xFF5B4042;
            INK_3 = 0xFF8C7072;
            INK_4 = 0xFFBFA5A7;
            PINK = 0xFFB3261E;
            PINK_DEEP = 0xFF8C1D18;
            PINK_SOFT = 0xFFF9D8DA;
            PINK_TINT = 0xFFFDEDEE;
            PINK_LINE = 0xFFEECACD;
            USER_BUBBLE = 0xFFF9D8DA;
            BOT_BUBBLE = 0xFFFFFFFF;
            LINE = 0xFFE8D5D7;
            LINE_2 = 0xFFF1E3E5;
            DARK = 0xFF2B2223;
            CODE_BG = 0xFFF8EFF0;
            CODE_HEAD = 0xFFF2E4E6;
            SHADOW = 0x142A1B22;
            SCRIM = 0x66150D0F;
            return;
        }
        int a = Theme.accent(themeIndex);
        PINK = a;
        PINK_DEEP = mix(a, 0xFF000000, 0.86f);
        PINK_SOFT = mix(a, 0xFFFFFFFF, 0.15f);
        PINK_TINT = mix(a, 0xFFFFFFFF, 0.07f);
        PINK_LINE = mix(a, 0xFFFFFFFF, 0.32f);
        USER_BUBBLE = mix(a, 0xFFFFFFFF, 0.20f);

        BG = Theme.bg(themeIndex);
        BG_DEEP = mix(BG, 0xFF000000, 0.965f);
        LINE = mix(a, 0xFFFFFFFF, 0.12f);
        LINE_2 = mix(a, 0xFFFFFFFF, 0.07f);
        CODE_BG = mix(BG, 0xFF000000, 0.975f);
        CODE_HEAD = mix(BG, 0xFF000000, 0.945f);
    }

    /**
     * 线性混合：weight 为 colorA 的占比。
     * 用整数运算算通道，避免引入 ColorUtils（support 库，禁止）。
     */
    public static int mix(int colorA, int colorB, float weight) {
        if (weight < 0f) weight = 0f;
        if (weight > 1f) weight = 1f;
        int a = (colorA >>> 24) & 0xff;
        int ar = (colorA >> 16) & 0xff;
        int ag = (colorA >> 8) & 0xff;
        int ab = colorA & 0xff;
        int br = (colorB >> 16) & 0xff;
        int bg = (colorB >> 8) & 0xff;
        int bb = colorB & 0xff;
        int r = (int) (ar * weight + br * (1f - weight) + 0.5f);
        int g = (int) (ag * weight + bg * (1f - weight) + 0.5f);
        int b = (int) (ab * weight + bb * (1f - weight) + 0.5f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
