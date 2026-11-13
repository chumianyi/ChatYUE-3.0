package com.chatyue.app.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 最小的文件 ContentProvider。
 *
 * Android 7.0 起 file:// 不能再跨应用分享，而本工程不允许引入 AndroidX 的 FileProvider，
 * 所以这里手写一个：只读、导出为 false、按需授予临时读权限，并且严格限制在应用可访问目录内。
 */
public class AgentFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.chatyue.app.files";

    public static Uri uriFor(File file) {
        return Uri.parse("content://" + AUTHORITY + "/" + Uri.encode(file.getAbsolutePath(), "/"));
    }

    public static String typeFor(String name) {
        String ext = com.chatyue.app.data.AgentTools.ext(name);
        if ("png".equals(ext)) return "image/png";
        if ("jpg".equals(ext) || "jpeg".equals(ext)) return "image/jpeg";
        if ("gif".equals(ext)) return "image/gif";
        if ("webp".equals(ext)) return "image/webp";
        if ("pdf".equals(ext)) return "application/pdf";
        if ("html".equals(ext) || "htm".equals(ext)) return "text/html";
        if ("txt".equals(ext) || "md".equals(ext) || "log".equals(ext)) return "text/plain";
        if ("json".equals(ext)) return "application/json";
        if ("zip".equals(ext)) return "application/zip";
        if ("apk".equals(ext)) return "application/vnd.android.package-archive";
        if ("mp3".equals(ext)) return "audio/mpeg";
        if ("mp4".equals(ext)) return "video/mp4";
        return "application/octet-stream";
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        File f = resolve(uri);
        if (f == null || !f.exists()) return null;
        String[] cols = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cur = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = Long.valueOf(f.length());
            else row[i] = null;
        }
        cur.addRow(row);
        return cur;
    }

    @Override
    public String getType(Uri uri) {
        File f = resolve(uri);
        return f == null ? null : typeFor(f.getName());
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = resolve(uri);
        if (f == null || !f.exists() || !f.isFile()) throw new FileNotFoundException(String.valueOf(uri));
        try {
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Exception e) {
            throw new FileNotFoundException("无法打开：" + f.getAbsolutePath());
        }
    }

    /** 只允许应用私有目录、应用外部目录和外部存储根目录。 */
    private File resolve(Uri uri) {
        try {
            String p = uri.getPath();
            if (p == null || p.length() == 0) return null;
            if (p.startsWith("/")) p = p.substring(1);
            File f = new File(p);
            String path = f.getCanonicalPath();
            String[] roots = allowedRoots();
            for (int i = 0; i < roots.length; i++) {
                if (roots[i] == null) continue;
                String root = new File(roots[i]).getCanonicalPath();
                if (path.equals(root) || path.startsWith(root + File.separator)) return f;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String[] allowedRoots() {
        if (getContext() == null) return new String[0];
        String ext = null;
        try {
            ext = Environment.getExternalStorageDirectory().getAbsolutePath();
        } catch (Throwable ignored) {
        }
        String appExt = null;
        try {
            File f = getContext().getExternalFilesDir(null);
            if (f != null) appExt = f.getAbsolutePath();
        } catch (Throwable ignored) {
        }
        return new String[]{
                getContext().getFilesDir().getAbsolutePath(),
                getContext().getCacheDir().getAbsolutePath(),
                appExt,
                ext
        };
    }
}
