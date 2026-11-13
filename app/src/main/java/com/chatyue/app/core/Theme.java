package com.chatyue.app.core;

/**
 * 13 套主题（含经典 Material 3）。只定义主色与底色，其余派生色由 {@link Palette#apply(int)} 计算，
 * 保证每一套配色的层级关系（主色 / 深色 / 浅色 / 描边 / 气泡）都协调。
 */
public final class Theme {

    public static final String[] NAMES = {
            "樱花", "黑白", "暖纸", "海洋", "春", "秋",
            "墨", "和纸", "蓝染", "暮紫韵", "琥珀金", "暮玫瑰", "经典"
    };

    /** 主色（accent）。 */
    private static final int[] ACCENT = {
            0xFFEF7FA8, // 樱花
            0xFF2B2A2D, // 黑白
            0xFF6E8B74, // 暖纸
            0xFF3E7BD6, // 海洋
            0xFF56AE62, // 春
            0xFFE1834A, // 秋
            0xFF4A4A4E, // 墨
            0xFFA98A5B, // 和纸
            0xFF33557F, // 蓝染
            0xFF7B6BC4, // 暮紫韵
            0xFFC79A2E, // 琥珀金
            0xFFC4707F, // 暮玫瑰
            0xFFB3261E  // 经典（Material 3 红）
    };

    /** 页面底色，随主题微调，保持极浅、低饱和。 */
    private static final int[] BG = {
            0xFFF6F3F5,
            0xFFF4F4F5,
            0xFFF7F3EA,
            0xFFF3F6FB,
            0xFFF3F8F3,
            0xFFFAF4EF,
            0xFFF4F3F5,
            0xFFF8F4EC,
            0xFFF2F5F9,
            0xFFF5F3FA,
            0xFFFAF6EA,
            0xFFF9F2F3,
            0xFFFDF7F8  // 经典（M3 粉白表面）
    };

    public static final int CLASSIC = 12;

    public static final int COUNT = NAMES.length;

    private Theme() {
    }

    public static int clamp(int index) {
        if (index < 0) return 0;
        if (index >= COUNT) return 0;
        return index;
    }

    public static String name(int index) {
        return NAMES[clamp(index)];
    }

    public static int accent(int index) {
        return ACCENT[clamp(index)];
    }

    public static int bg(int index) {
        return BG[clamp(index)];
    }

    /** 主题色块预览用：与白色混合 78%，得到浅一档的展示色。 */
    public static int preview(int index) {
        return Palette.mix(ACCENT[clamp(index)], 0xFFFFFFFF, 0.78f);
    }
}
