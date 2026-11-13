package com.chatyue.app.crash;

import android.content.Context;
import android.content.Intent;
import android.os.Process;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.chatyue.app.data.Prefs;
import com.chatyue.app.util.FileUtil;
import com.chatyue.app.ErrorActivity;

/**
 * 全局兜底：任何未捕获异常都会落到独立的错误页进程，
 * 主进程随后退出，但用户看到的是可复制日志、可重试、可返回的界面，而不是系统「已停止运行」。
 */
public final class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final Object LOCK = new Object();
    private static CrashHandler sInstance;
    private final Thread.UncaughtExceptionHandler previous;
    private final Context ctx;

    private CrashHandler(Context c, Thread.UncaughtExceptionHandler prev) {
        ctx = c.getApplicationContext();
        previous = prev;
    }

    public static void install(Context c) {
        if (sInstance != null) return;
        Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        sInstance = new CrashHandler(c, prev);
        Thread.setDefaultUncaughtExceptionHandler(sInstance);
    }

    public void uncaughtException(Thread thread, Throwable ex) {
        String log = buildLog(thread, ex);
        String path = writeLog(log);
        try {
            Intent i = new Intent(ctx, ErrorActivity.class);
            i.putExtra("log", log);
            i.putExtra("path", path);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Throwable t) {
            // 错误页自己都起不来，只能交回系统
            if (previous != null) {
                previous.uncaughtException(thread, ex);
                return;
            }
        }
        try {
            Thread.sleep(450);
        } catch (Throwable ignored) {
        }
        try {
            Process.killProcess(Process.myPid());
        } catch (Throwable ignored) {
        }
        System.exit(10);
    }

    private String buildLog(Thread thread, Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            pw.println("ChatYUE 3.0 崩溃报告");
            pw.println("时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            pw.println("线程：" + (thread == null ? "unknown" : thread.getName()));
            pw.println("设备：" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                    + " · Android " + android.os.Build.VERSION.RELEASE
                    + "（API " + android.os.Build.VERSION.SDK + "）");
            pw.println("应用：com.chatyue.app · 3.0");
            pw.println();
            if (ex != null) ex.printStackTrace(pw);
            Throwable cause = ex == null ? null : ex.getCause();
            int guard = 0;
            while (cause != null && guard++ < 4) {
                pw.println();
                pw.println("Caused by:");
                cause.printStackTrace(pw);
                cause = cause.getCause();
            }
        } catch (Throwable ignored) {
        }
        try {
            pw.flush();
        } catch (Throwable ignored) {
        }
        String s = sw.toString();
        if (s.length() > 200000) s = s.substring(0, 200000) + "\n…（日志过长，已截断）";
        return s;
    }

    private String writeLog(String log) {
        synchronized (LOCK) {
            try {
                File dir = new File(ctx.getFilesDir(), "crash");
                FileUtil.mkdirs(dir);
                SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd_HHmmss");
                File file = new File(dir, "crash_" + f.format(new Date()) + ".txt");
                FileUtil.writeText(file, log);
                // 只保留最近 8 份，避免占满空间
                File[] all = dir.listFiles();
                if (all != null && all.length > 8) {
                    java.util.Arrays.sort(all, new java.util.Comparator<File>() {
                        public int compare(File a, File b) {
                            return a.lastModified() > b.lastModified() ? -1 : 1;
                        }
                    });
                    for (int i = 8; i < all.length; i++) FileUtil.delete(all[i]);
                }
                Prefs.setString(ctx, Prefs.KEY_CRASH_LAST, file.getAbsolutePath());
                return file.getAbsolutePath();
            } catch (Throwable t) {
                return "";
            }
        }
    }
}
