package com.chatyue.app.content;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.chatyue.app.data.Experiment;
import com.chatyue.app.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/** 产物的分享与保存。 */
public final class FileSharing {

    private FileSharing() {
    }

    public static void share(Context c, ArrayList<File> files) {
        if (files == null || files.isEmpty()) return;
        try {
            Intent intent;
            ArrayList<Uri> uris = new ArrayList<Uri>();
            for (int i = 0; i < files.size(); i++) {
                File f = files.get(i);
                Uri u = uriFor(c, f);
                if (u != null) uris.add(u);
            }
            if (uris.isEmpty()) return;
            if (uris.size() == 1) {
                intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                intent.setType(AgentFileProvider.typeFor(files.get(0).getName()));
            } else {
                intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                intent.setType("*/*");
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, "分享产物");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(chooser);
        } catch (Throwable ignored) {
        }
    }

    public static Uri uriFor(Context c, File f) {
        try {
            if (Experiment.sdkInt() >= 24) return AgentFileProvider.uriFor(f);
            return Uri.fromFile(f);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 保存到公共下载目录。成功返回保存后的文件，失败返回 null。
     * Android 11+ 需要「所有文件」权限；没有权限时由调用方引导授权。
     */
    public static File saveToDownloads(Context c, File src) {
        if (src == null || !src.exists() || !src.isFile()) return null;
        File outDir = null;
        try {
            outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } catch (Throwable ignored) {
        }
        if (outDir == null) {
            File ext = Environment.getExternalStorageDirectory();
            outDir = new File(ext, "Download");
        }
        File target = new File(outDir, src.getName());
        if (target.exists()) {
            target = com.chatyue.app.data.Attachments.unique(outDir, src.getName());
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            FileUtil.mkdirs(outDir);
            in = new FileInputStream(src);
            out = new FileOutputStream(target);
            FileUtil.copy(in, out);
            return target;
        } catch (Throwable t) {
            return null;
        } finally {
            FileUtil.close(in);
            FileUtil.close(out);
        }
    }

    /**
     * Android 10+ 的 MediaStore 下载目录写入：不需要「所有文件」权限，
     * 系统会把它归到 Download/ChatYUE 下。成功返回展示用路径。
     */
    public static String saveViaMediaStore(Context c, File src) {
        if (src == null || !src.exists()) return null;
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("_display_name", src.getName());
        v.put("mime_type", AgentFileProvider.typeFor(src.getName()));
        if (Experiment.sdkInt() >= 29) {
            v.put("relative_path", "Download/ChatYUE");
        }
        android.net.Uri collection = Uri.parse("content://media/external/downloads");
        android.net.Uri item = null;
        try {
            item = c.getContentResolver().insert(collection, v);
            if (item == null) return null;
            java.io.OutputStream out = c.getContentResolver().openOutputStream(item);
            if (out == null) return null;
            FileInputStream in = new FileInputStream(src);
            try {
                FileUtil.copy(in, out);
            } finally {
                FileUtil.close(in);
                FileUtil.close((java.io.OutputStream) out);
            }
            return "Download/ChatYUE/" + src.getName();
        } catch (Throwable t) {
            if (item != null) {
                try {
                    c.getContentResolver().delete(item, null, null);
                } catch (Throwable ignored) {
                }
            }
            return null;
        }
    }

    /** 保存到应用自己的外部目录（不需要任何权限，卸载时一起删除）。 */
    public static File saveToAppStorage(Context c, File src) {        try {
            File base = c.getExternalFilesDir(null);
            if (base == null) base = new File(c.getFilesDir(), "exports");
            File outDir = new File(base, "ChatYUE");
            FileUtil.mkdirs(outDir);
            File target = new File(outDir, src.getName());
            if (target.exists()) target = com.chatyue.app.data.Attachments.unique(outDir, src.getName());
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(target);
            try {
                FileUtil.copy(in, out);
            } finally {
                FileUtil.close(in);
                FileUtil.close(out);
            }
            return target;
        } catch (Throwable t) {
            return null;
        }
    }
}
