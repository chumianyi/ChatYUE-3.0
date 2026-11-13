package com.chatyue.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 低版本没有 Okio / java.nio.file，文件读写全部走最基础的字节流。
 */
public final class FileUtil {

    private FileUtil() {
    }

    public static void writeText(File f, String text) throws IOException {
        mkdirs(f.getParentFile());
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
            out.flush();
        } finally {
            close(out);
        }
    }

    public static String readText(File f, int maxBytes) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            return new String(readLimited(in, maxBytes), "UTF-8");
        } finally {
            close(in);
        }
    }

    private static byte[] readLimited(InputStream in, int maxBytes) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
            if (total + n > maxBytes) {
                n = maxBytes - total;
                if (n <= 0) break;
            }
            bos.write(buf, 0, n);
            total += n;
            if (total >= maxBytes) break;
        }
        return bos.toByteArray();
    }

    public static String readText(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            return new String(readAll(in), "UTF-8");
        } finally {
            close(in);
        }
    }

    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        out.flush();
    }

    public static void mkdirs(File dir) {
        if (dir != null && !dir.exists()) dir.mkdirs();
    }

    public static boolean delete(File f) {
        if (f == null || !f.exists()) return false;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (int i = 0; i < kids.length; i++) delete(kids[i]);
            }
        }
        return f.delete();
    }

    public static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static void close(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static String readAsset(Context c, String name) {
        InputStream in = null;
        try {
            in = c.getAssets().open(name);
            return new String(readAll(in), "UTF-8");
        } catch (Exception e) {
            return null;
        } finally {
            close(in);
        }
    }

    /** 把图片压缩成 JPEG 字节，用于视觉输入（base64 data URL）。 */
    public static byte[] compressJpeg(Bitmap bmp, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        return bos.toByteArray();
    }

    /** 按最长边缩放解码，避免大图 OOM——低版本堆很小，必须自己做采样。 */
    public static Bitmap decodeSampled(InputStream in, int maxSide) {
        try {
            // 先读入内存，才能两遍解析（老版本不支持 mark/reset 的流会被复用）
            byte[] data = readAll(in);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int sample = 1;
            int side = Math.max(o.outWidth, o.outHeight);
            while (side / sample > maxSide * 2) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            o2.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, o2);
            if (bmp == null) return null;
            int max = Math.max(bmp.getWidth(), bmp.getHeight());
            if (max > maxSide) {
                float scale = maxSide / (float) max;
                Bitmap scaled = Bitmap.createScaledBitmap(bmp,
                        Math.max(1, (int) (bmp.getWidth() * scale)),
                        Math.max(1, (int) (bmp.getHeight() * scale)), true);
                if (scaled != bmp) bmp.recycle();
                return scaled;
            }
            return bmp;
        } catch (Exception e) {
            return null;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }
}
