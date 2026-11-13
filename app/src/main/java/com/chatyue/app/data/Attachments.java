package com.chatyue.app.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.chatyue.app.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** 任意附件导入：流式复制，不设体积上限（只受设备剩余空间限制）。 */
public final class Attachments {

    private Attachments() {
    }

    public static File dir(Context c) {
        File d = new File(c.getFilesDir(), "attachments");
        FileUtil.mkdirs(d);
        return d;
    }

    public static String displayName(Context c, Uri uri) {
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(uri, null, null, null, null);
            if (cur != null && cur.moveToFirst()) {
                int idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String n = cur.getString(idx);
                    if (n != null && n.length() > 0) return n;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (cur != null) {
                try {
                    cur.close();
                } catch (Throwable ignored) {
                }
            }
        }
        String last = uri.getLastPathSegment();
        return last == null || last.length() == 0 ? "附件" : last;
    }

    public static File importUri(Context c, Uri uri, String name) throws Exception {
        String safe = sanitize(name);
        File target = unique(dir(c), safe);
        InputStream in = c.getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("无法读取这个文件");
        FileOutputStream out = new FileOutputStream(target);
        try {
            FileUtil.copy(in, out);
        } finally {
            FileUtil.close(in);
            FileUtil.close(out);
        }
        return target;
    }

    public static File unique(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        for (int i = 1; i < 1000; i++) {
            f = new File(dir, base + " (" + i + ")" + ext);
            if (!f.exists()) return f;
        }
        return new File(dir, System.currentTimeMillis() + ext);
    }

    public static String sanitize(String name) {
        String n = name == null ? "附件" : name.trim();
        if (n.length() == 0) n = "附件";
        n = n.replace('/', '_').replace('\\', '_').replace('\n', '_');
        if (n.length() > 96) n = n.substring(0, 96);
        return n;
    }

    public static boolean isImage(String name) {
        String ext = AgentTools.ext(name);
        return "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)
                || "gif".equals(ext) || "webp".equals(ext) || "bmp".equals(ext);
    }

    /** 给模型的附件说明：文件名、大小、类型；小文本直接附上内容。 */
    public static String promptBrief(Context c, java.util.ArrayList<String> paths) {
        if (paths == null || paths.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("用户本条消息附带了以下文件（路径都在本机）：\n");
        int textBudget = 40 * 1024;
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (p == null || p.length() == 0) continue;
            File f = new File(p);
            if (!f.exists()) continue;
            sb.append("- ").append(f.getName()).append("（")
                    .append(AgentTools.sizeText(f.length())).append("，")
                    .append(f.getAbsolutePath()).append("）\n");
            if (isText(f.getName()) && f.length() <= textBudget && f.length() <= 24 * 1024) {
                try {
                    sb.append("  内容：\n```\n").append(FileUtil.readText(f)).append("\n```\n");
                    textBudget -= (int) f.length();
                } catch (Throwable ignored) {
                }
            }
        }
        sb.append("如果是图片，会另外以视觉输入发送；需要修改或读取更多文件时，"
                + "在 Agent 模式下可以调用 read_file 工具。");
        return sb.toString();
    }

    public static boolean isText(String name) {
        String ext = AgentTools.ext(name);
        return "txt".equals(ext) || "md".equals(ext) || "json".equals(ext) || "xml".equals(ext)
                || "html".equals(ext) || "htm".equals(ext) || "css".equals(ext) || "js".equals(ext)
                || "java".equals(ext) || "c".equals(ext) || "h".equals(ext) || "py".equals(ext)
                || "log".equals(ext) || "csv".equals(ext) || "yml".equals(ext) || "yaml".equals(ext)
                || "ini".equals(ext) || "gradle".equals(ext) || "sh".equals(ext);
    }
}
