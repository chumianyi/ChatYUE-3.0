package com.chatyue.app.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.lang.reflect.Method;

/**
 * 3.0 的实验室能力：反思 / 多轮 Agent / 全部文件。
 *
 * 这里只做「开关 + 能力判定」，不直接碰 UI。
 * 所有 Android 5.0+ 才存在的接口都用反射或字符串常量，保证在 API 3 的设备上
 * 连类加载都不会因为缺少新 API 而失败。
 */
public final class Experiment {

    public static final int REQ_STORAGE = 1201;
    public static final int REQ_ALL_FILES = 1202;

    private Experiment() {
    }

    /** API 1 就有 Build.VERSION.SDK（字符串），避免静态引用 API 4 的 SDK_INT。 */
    public static int sdkInt() {
        try {
            return Integer.parseInt(Build.VERSION.SDK);
        } catch (Throwable t) {
            return 3;
        }
    }

    /** 全部文件能力只支持 Android 5.0（API 21）以上。 */
    public static boolean supportsAllFiles() {
        return sdkInt() >= 21;
    }

    public static boolean agentReady(Context c) {
        return Prefs.agentEnabled(c);
    }

    public static boolean agentEnabled(Context c) {
        return Prefs.agentEnabled(c);
    }

    public static boolean allFilesReady(Context c) {
        return Prefs.agentEnabled(c) && Prefs.allFilesEnabled(c)
                && supportsAllFiles() && hasAllFilesPermission(c);
    }

    /** 用 API 1 就存在的 PackageManager.checkPermission 查询运行时权限。 */
    public static boolean granted(Context c, String permission) {
        try {
            PackageManager pm = c.getPackageManager();
            return pm.checkPermission(permission, c.getPackageName())
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Android 11+ 的「所有文件访问」；旧系统返回 false，由 granted() 判断。 */
    public static boolean hasAllFilesPermission(Context c) {
        if (!supportsAllFiles()) return false;
        if (sdkInt() >= 30) {
            try {
                Class env = Class.forName("android.os.Environment");
                Method m = env.getMethod("isExternalStorageManager", new Class[0]);
                Object v = m.invoke(null, new Object[0]);
                return v instanceof Boolean && ((Boolean) v).booleanValue();
            } catch (Throwable t) {
                return false;
            }
        }
        return granted(c, "android.permission.READ_EXTERNAL_STORAGE")
                && granted(c, "android.permission.WRITE_EXTERNAL_STORAGE");
    }

    /**
     * 打开「所有文件访问」授权页。
     * Android 11 的 action 是普通字符串，不静态引用 Settings 里的新常量。
     */
    public static boolean openAllFilesSettings(Context c) {
        try {
            Intent i = new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION");
            i.setData(Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Throwable t) {
            try {
                Intent i = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                i.setData(Uri.parse("package:" + c.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return true;
            } catch (Throwable t2) {
                try {
                    Intent i = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                    i.setData(Uri.parse("package:" + c.getPackageName()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                    return true;
                } catch (Throwable t3) {
                    return false;
                }
            }
        }
    }

    /** 反射申请运行时权限（requestPermissions 是 API 23 才有的）。 */
    public static void requestPermissions(Object activity, String[] perms, int requestCode) {
        if (activity == null) return;
        try {
            Class ac = Class.forName("android.app.Activity");
            Method m = ac.getMethod("requestPermissions",
                    new Class[]{String[].class, Integer.TYPE});
            m.invoke(activity, new Object[]{perms, Integer.valueOf(requestCode)});
        } catch (Throwable ignored) {
        }
    }

    /** 设备是否已经有可写的公共存储（用于产物「保存到本地」）。 */
    public static boolean canWritePublic(Context c) {
        int sdk = sdkInt();
        if (sdk >= 30) return true; // 走 MediaStore.Downloads，无需权限
        if (sdk >= 23) {
            return granted(c, "android.permission.WRITE_EXTERNAL_STORAGE");
        }
        return true;
    }

    public static boolean mediaStoreAvailable() {
        return sdkInt() >= 29;
    }

    /** 外部存储根目录；没有挂载时退回应用私有目录。 */
    public static java.io.File externalRoot(Context c) {
        try {
            java.io.File f = Environment.getExternalStorageDirectory();
            if (f != null && f.exists()) return f;
        } catch (Throwable ignored) {
        }
        return c.getFilesDir();
    }
}
