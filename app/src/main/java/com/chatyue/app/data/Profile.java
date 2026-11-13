package com.chatyue.app.data;

import android.content.Context;

/**
 * 用户画像：称呼、身份、偏好、沟通风格。
 * 每次请求会作为一段「关于用户」的补充信息拼进系统提示词。
 */
public final class Profile {

    private Profile() {
    }

    public static String compose(Context c) {
        StringBuilder sb = new StringBuilder();
        String nick = Prefs.getString(c, Prefs.KEY_NICKNAME, "");
        String role = Prefs.getString(c, Prefs.KEY_PROFILE_ROLE, "");
        String traits = Prefs.getString(c, Prefs.KEY_PROFILE_TRAITS, "");
        String style = Prefs.getString(c, Prefs.KEY_PROFILE_STYLE, "");

        if (nick != null && nick.length() > 0) sb.append("- 称呼用户为「").append(nick).append("」\n");
        if (role != null && role.length() > 0) sb.append("- 用户的身份/职业：").append(role).append("\n");
        if (traits != null && traits.length() > 0) sb.append("- 偏好与禁忌：").append(traits).append("\n");
        if (style != null && style.length() > 0) sb.append("- 期望的表达风格：").append(style).append("\n");

        if (sb.length() == 0) return "";
        return "关于用户（不主动复述，只用于调整回答）：\n" + sb.toString();
    }
}
