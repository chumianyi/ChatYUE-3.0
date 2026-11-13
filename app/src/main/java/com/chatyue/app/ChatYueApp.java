package com.chatyue.app;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.net.WolfSsl;
import com.chatyue.app.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ChatYueApp extends Application {

    private static Context sContext;
    private static Bitmap sLogo;

    @Override
    public void onCreate() {
        super.onCreate();
        // 第一件事：装好崩溃兜底，后面的初始化再出问题也有错误页接住。
        com.chatyue.app.crash.CrashHandler.install(this);
        sContext = this;
        Ui.init(this);
        // 主题必须在任何控件构造之前落到 Palette 上（颜色字段是非 final 的运行时值）
        Palette.apply(Prefs.theme(this));
        Prefs.ensureFirstLaunch(this);
        prepareTls();
    }

    public static Context ctx() {
        return sContext;
    }

    /**
     * 把内置 CA 包释放到私有目录，交给 wolfSSL 做证书链校验。
     * 老系统的 /system/etc/security/cacerts 早已过期，不能依赖。
     */
    private void prepareTls() {
        File out = new File(getFilesDir(), "cacert.pem");
        if (!out.exists() || out.length() < 1024) {
            InputStream in = null;
            FileOutputStream fos = null;
            try {
                in = getAssets().open("cacert.pem");
                fos = new FileOutputStream(out);
                FileUtil.copy(in, fos);
            } catch (Exception ignored) {
            } finally {
                FileUtil.close(in);
                FileUtil.close(fos);
            }
        }
        WolfSsl.setCaBundle(out.getAbsolutePath());
    }

    public static Bitmap logo(Context c) {
        if (sLogo == null) {
            try {
                sLogo = BitmapFactory.decodeResource(c.getResources(), R.drawable.ic_launcher);
            } catch (Throwable ignored) {
            }
        }
        return sLogo;
    }
}
