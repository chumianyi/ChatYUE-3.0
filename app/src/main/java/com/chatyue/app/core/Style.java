package com.chatyue.app.core;

import android.content.Context;

import com.chatyue.app.data.Prefs;

/** 视觉体系路由。经典主题不是换色，而是整套 Material 3 页面。 */
public final class Style {

    private Style() {
    }

    public static boolean classic(Context c) {
        return Prefs.theme(c) == Theme.CLASSIC;
    }
}
