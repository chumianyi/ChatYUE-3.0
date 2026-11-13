package com.chatyue.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static String hhmm(long t) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.CHINA);
        return f.format(new Date(t));
    }

    /** 首页那种「上午 9:03 / 昨天 / 11月12日」的友好时间。 */
    public static String friendly(long t) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        boolean sameDay = now.get(Calendar.YEAR) == c.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
        if (sameDay) return hhmm(t);
        now.add(Calendar.DAY_OF_YEAR, -1);
        boolean yesterday = now.get(Calendar.YEAR) == c.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
        if (yesterday) return "昨天";
        int days = (int) ((System.currentTimeMillis() - t) / 86400000L);
        if (days < 7) return days + " 天前";
        SimpleDateFormat f = new SimpleDateFormat("MM月dd日", Locale.CHINA);
        return f.format(new Date(t));
    }

    /** 中文长日期：11月12日 星期四 */
    public static String dateLine() {
        Calendar c = Calendar.getInstance();
        String[] week = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        return (c.get(Calendar.MONTH) + 1) + "月" + c.get(Calendar.DAY_OF_MONTH) + "日 "
                + week[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 6) return "夜深了";
        if (h < 11) return "早上好";
        if (h < 14) return "中午好";
        if (h < 18) return "下午好";
        return "晚上好";
    }

    /** 陪伴天数，从首次启动那天算起。 */
    public static int daysSince(long start) {
        if (start <= 0) return 1;
        long d = (System.currentTimeMillis() - start) / 86400000L;
        return (int) Math.max(1, d + 1);
    }
}
