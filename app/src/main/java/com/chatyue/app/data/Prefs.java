package com.chatyue.app.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 全局偏好。SharedPreferences 从 API 1 就有，安全可用。
 */
public final class Prefs {

    private static final String FILE = "chatyue_prefs";

    public static final String KEY_PROVIDER = "default_provider";
    public static final String KEY_CUSTOM_PROVIDERS = "custom_providers";
    public static final String KEY_SYSTEM_PROMPT = "system_prompt";
    public static final String KEY_MEMORY = "memory_enabled";
    public static final String KEY_FONT = "font_scale";
    public static final String KEY_NICKNAME = "nickname";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_TLS_FALLBACK = "tls_fallback";
    public static final String KEY_LAST_MODEL = "last_model";
    public static final String KEY_THEME = "theme_index";
    public static final String KEY_LANG = "language";
    public static final String KEY_ONBOARDED = "onboarded";
    public static final String KEY_ENTER_SEND = "enter_send";
    public static final String KEY_AUTOSCROLL = "auto_scroll";
    public static final String KEY_CONTEXT = "context_rounds";
    public static final String KEY_TEMP = "temperature_x100";
    public static final String KEY_TOPP = "top_p_x100";
    public static final String KEY_MAXTOKENS = "max_tokens";
    public static final String KEY_NOTIFY = "notify_enabled";
    public static final String KEY_SKILLS = "enabled_skills";
    public static final String KEY_QUICK = "quick_messages";
    public static final String KEY_ASSISTANTS = "custom_assistants";
    public static final String KEY_ASSISTANT = "default_assistant";
    public static final String KEY_AGENT_WEB = "agent_web";
    public static final String KEY_AGENT_CODE = "agent_code";
    public static final String KEY_AGENT_PROACTIVE = "agent_proactive";
    public static final String KEY_AGENT_COLLAB = "agent_collab";
    public static final String KEY_PROFILE_ROLE = "profile_role";
    public static final String KEY_PROFILE_STYLE = "profile_style";
    public static final String KEY_PROFILE_TRAITS = "profile_traits";
    public static final String KEY_APPEARANCE_RADIUS = "appearance_radius";
    public static final String KEY_BUBBLE_STYLE = "bubble_style";
    // ---- 3.0 实验性功能 / Agent ----
    public static final String KEY_EXP_REFLECTION = "exp_reflection";
    public static final String KEY_EXP_AGENT = "exp_agent";
    public static final String KEY_EXP_ALL_FILES = "exp_all_files";
    public static final String KEY_EXP_ONBOARD_TIP = "exp_onboard_tip";
    public static final String KEY_THINKING = "thinking_";
    public static final String KEY_WORKSPACE_LEGACY = "workspace_all_files";
    public static final String KEY_CRASH_LAST = "crash_last_path";
    public static final String KEY_AGENT_MAX_ROUNDS = "agent_max_rounds";

    private Prefs() {
    }

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getString(Context c, String key, String def) {
        return sp(c).getString(key, def);
    }

    public static void setString(Context c, String key, String v) {
        SharedPreferences.Editor e = sp(c).edit();
        e.putString(key, v);
        e.commit();
    }

    public static boolean getBool(Context c, String key, boolean def) {
        return sp(c).getBoolean(key, def);
    }

    public static void setBool(Context c, String key, boolean v) {
        SharedPreferences.Editor e = sp(c).edit();
        e.putBoolean(key, v);
        e.commit();
    }

    public static int getInt(Context c, String key, int def) {
        return sp(c).getInt(key, def);
    }

    public static void setInt(Context c, String key, int v) {
        SharedPreferences.Editor e = sp(c).edit();
        e.putInt(key, v);
        e.commit();
    }

    public static long getLong(Context c, String key, long def) {
        return sp(c).getLong(key, def);
    }

    public static void setLong(Context c, String key, long v) {
        SharedPreferences.Editor e = sp(c).edit();
        e.putLong(key, v);
        e.commit();
    }

    public static void ensureFirstLaunch(Context c) {
        long t = getLong(c, KEY_FIRST_LAUNCH, 0);
        if (t == 0) setLong(c, KEY_FIRST_LAUNCH, System.currentTimeMillis());
    }

    public static long firstLaunch(Context c) {
        return getLong(c, KEY_FIRST_LAUNCH, System.currentTimeMillis());
    }

    public static float fontScale(Context c) {
        int v = getInt(c, KEY_FONT, 100);
        if (v <= 0) v = 100;
        return v / 100f;
    }

    public static int fontPercent(Context c) {
        int v = getInt(c, KEY_FONT, 100);
        return v <= 0 ? 100 : v;
    }

    public static void setFontPercent(Context c, int percent) {
        if (percent < 85) percent = 85;
        if (percent > 130) percent = 130;
        setInt(c, KEY_FONT, percent);
    }

    public static int theme(Context c) {
        return getInt(c, KEY_THEME, 0);
    }

    public static void setTheme(Context c, int index) {
        setInt(c, KEY_THEME, index);
    }

    public static boolean onboarded(Context c) {
        return getBool(c, KEY_ONBOARDED, false);
    }

    public static void setOnboarded(Context c, boolean v) {
        setBool(c, KEY_ONBOARDED, v);
    }

    public static String language(Context c) {
        return getString(c, KEY_LANG, "简体中文");
    }

    public static int temperature(Context c) {
        return getInt(c, KEY_TEMP, 70);
    }

    public static int topP(Context c) {
        return getInt(c, KEY_TOPP, 90);
    }

    public static int maxTokens(Context c) {
        return getInt(c, KEY_MAXTOKENS, 2048);
    }

    public static int contextRounds(Context c) {
        return getInt(c, KEY_CONTEXT, 12);
    }

    public static void cycleFont(Context c) {
        int cur = getInt(c, KEY_FONT, 100);
        int next = cur == 100 ? 112 : (cur == 112 ? 88 : 100);
        setInt(c, KEY_FONT, next);
    }

    public static String fontLabel(Context c) {
        int cur = getInt(c, KEY_FONT, 100);
        if (cur == 112) return "大";
        if (cur == 88) return "小";
        return "标准";
    }

    public static String nickname(Context c) {
        String n = getString(c, KEY_NICKNAME, "");
        return n == null || n.length() == 0 ? "主人" : n;
    }

    // ------------------------------------------------ 3.0 实验性功能

    public static boolean reflectionEnabled(Context c) {
        return getBool(c, KEY_EXP_REFLECTION, false);
    }

    public static void setReflectionEnabled(Context c, boolean v) {
        setBool(c, KEY_EXP_REFLECTION, v);
    }

    public static boolean agentEnabled(Context c) {
        return getBool(c, KEY_EXP_AGENT, false);
    }

    public static void setAgentEnabled(Context c, boolean v) {
        setBool(c, KEY_EXP_AGENT, v);
        if (!v) setBool(c, KEY_EXP_ALL_FILES, false);
    }

    public static boolean allFilesEnabled(Context c) {
        return getBool(c, KEY_EXP_ALL_FILES, false);
    }

    public static void setAllFilesEnabled(Context c, boolean v) {
        setBool(c, KEY_EXP_ALL_FILES, v);
    }

    /** 第三方模型可以关闭思考；内置模型本身就是思考模型，恒为 true。 */
    public static boolean thinkingEnabled(Context c, Provider p) {
        if (p == null || p.isBuiltin()) return true;
        return getBool(c, KEY_THINKING + p.id, true);
    }

    public static void setThinkingEnabled(Context c, Provider p, boolean v) {
        if (p == null || p.isBuiltin()) return;
        setBool(c, KEY_THINKING + p.id, v);
    }

    public static int agentMaxRounds(Context c) {
        int n = getInt(c, KEY_AGENT_MAX_ROUNDS, 12);
        if (n < 3) n = 3;
        if (n > 24) n = 24;
        return n;
    }

    public static String crashLogPath(Context c) {
        return getString(c, KEY_CRASH_LAST, "");
    }
}
