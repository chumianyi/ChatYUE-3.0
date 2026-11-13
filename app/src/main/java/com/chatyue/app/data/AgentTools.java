package com.chatyue.app.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.chatyue.app.util.FileUtil;

/**
 * Agent 的本地工具执行器。
 *
 * 所有路径都限制在「工作目录」内：
 * - 默认：应用私有 files/workspace
 * - 开启「全部文件」且拿到权限后：外部存储根目录
 * 工具只能读文本、写文本、创建/删除/移动、压缩与解压，不执行任何代码。
 */
public final class AgentTools {

    public static final String TOOL_LIST = "list_files";
    public static final String TOOL_READ = "read_file";
    public static final String TOOL_WRITE = "write_file";
    public static final String TOOL_APPEND = "append_file";
    public static final String TOOL_DELETE = "delete_file";
    public static final String TOOL_MKDIR = "make_dir";
    public static final String TOOL_MOVE = "move_file";
    public static final String TOOL_ZIP = "zip";
    public static final String TOOL_UNZIP = "unzip";
    public static final String TOOL_INFO = "file_info";

    private static final long MAX_READ = 400L * 1024L;
    private static final long MAX_TOTAL_UNZIP = 200L * 1024L * 1024L;
    private static final int MAX_ZIP_ENTRIES = 20000;

    private AgentTools() {
    }

    public static File root(Context c) {
        if (Experiment.allFilesReady(c)) {
            File ext = Experiment.externalRoot(c);
            if (ext != null) return ext;
        }
        File d = new File(c.getFilesDir(), "workspace");
        FileUtil.mkdirs(d);
        return d;
    }

    public static String rootLabel(Context c) {
        if (Experiment.allFilesReady(c)) return "设备存储根目录";
        return "应用工作目录";
    }

    /** 把用户/模型给的相对路径解析成工作目录内的真实路径。 */
    public static File resolve(Context c, String path) throws Exception {
        File root = root(c);
        String p = path == null ? "" : path.trim();
        if (p.length() == 0 || ".".equals(p) || "./".equals(p)) return root;
        File f;
        if (p.startsWith("/")) {
            f = new File(p);
        } else {
            f = new File(root, p);
        }
        String rootPath = root.getCanonicalPath();
        String target = f.getCanonicalPath();
        if (target.equals(rootPath) || target.startsWith(rootPath + File.separator)) return new File(target);
        // 用户导入的附件在 files/attachments，允许 Agent 按系统提示里的绝对路径读取。
        File attach = new File(c.getFilesDir(), "attachments");
        String attachPath = attach.getCanonicalPath();
        if (target.startsWith(attachPath + File.separator)) return new File(target);
        throw new SecurityException("路径越界，只能操作工作目录：" + p);
    }

    public static String relative(Context c, File f) {
        try {
            String root = root(c).getCanonicalPath();
            String p = f.getCanonicalPath();
            if (p.equals(root)) return ".";
            if (p.startsWith(root + File.separator)) return p.substring(root.length() + 1);
            return p;
        } catch (Exception e) {
            return f.getName();
        }
    }

    /** 执行一次工具调用；changed 里会收集本次写出的文件，供产物卡片使用。 */
    public static JSONObject execute(Context c, JSONObject call, ArrayList<String> changed) {
        JSONObject out = new JSONObject();
        try {
            String tool = call.optString("tool", call.optString("name", ""));
            String path = call.optString("path", "");
            if (TOOL_LIST.equals(tool)) {
                File dir = resolve(c, path.length() == 0 ? "." : path);
                if (!dir.exists()) throw new IOException("目录不存在：" + path);
                if (!dir.isDirectory()) throw new IOException("不是目录：" + path);
                String text = listTree(c, dir, 0, 3, 300);
                out.put("ok", true);
                out.put("output", text.length() == 0 ? "（空目录）" : text);
            } else if (TOOL_READ.equals(tool)) {
                File f = resolve(c, path);
                if (!f.exists() || !f.isFile()) throw new IOException("文件不存在：" + path);
                if (f.length() > MAX_READ) throw new IOException("文件超过 400 KB，请分段读取或压缩后处理");
                String text = FileUtil.readText(f);
                if (looksBinary(text)) throw new IOException("这是二进制文件，不能按文本读取：" + path);
                out.put("ok", true);
                out.put("output", text);
            } else if (TOOL_WRITE.equals(tool) || TOOL_APPEND.equals(tool)) {
                File f = resolve(c, path);
                if (f.isDirectory()) throw new IOException("目标是目录：" + path);
                FileUtil.mkdirs(f.getParentFile());
                String content = call.optString("content", "");
                if (TOOL_WRITE.equals(tool)) {
                    FileUtil.writeText(f, content);
                } else {
                    FileOutputStream fos = new FileOutputStream(f, true);
                    try {
                        fos.write(content.getBytes("UTF-8"));
                    } finally {
                        FileUtil.close(fos);
                    }
                }
                changed.add(f.getAbsolutePath());
                out.put("ok", true);
                out.put("output", (TOOL_WRITE.equals(tool) ? "已写入 " : "已追加 ") + relative(c, f)
                        + "（" + content.length() + " 字符）");
            } else if (TOOL_DELETE.equals(tool)) {
                File f = resolve(c, path);
                if (f.getCanonicalPath().equals(root(c).getCanonicalPath())) {
                    throw new IOException("不能删除工作目录本身");
                }
                if (!f.exists()) throw new IOException("不存在：" + path);
                boolean ok = FileUtil.delete(f);
                out.put("ok", ok);
                out.put("output", ok ? "已删除 " + path : "删除失败 " + path);
                if (ok) changed.add(f.getAbsolutePath());
            } else if (TOOL_MKDIR.equals(tool)) {
                File f = resolve(c, path);
                FileUtil.mkdirs(f);
                changed.add(f.getAbsolutePath());
                out.put("ok", true);
                out.put("output", "目录已就绪：" + relative(c, f));
            } else if (TOOL_MOVE.equals(tool)) {
                File from = resolve(c, call.optString("from", ""));
                File to = resolve(c, call.optString("to", ""));
                if (from.getCanonicalPath().equals(root(c).getCanonicalPath())) {
                    throw new IOException("不能移动工作目录本身");
                }
                if (!from.exists()) throw new IOException("源不存在：" + call.optString("from", ""));
                FileUtil.mkdirs(to.getParentFile());
                if (!from.renameTo(to)) {
                    copyRecursive(from, to);
                    FileUtil.delete(from);
                }
                changed.add(to.getAbsolutePath());
                out.put("ok", true);
                out.put("output", "已移动：" + relative(c, from) + " → " + relative(c, to));
            } else if (TOOL_ZIP.equals(tool)) {
                String output = call.optString("output", "archive.zip");
                File outFile = resolve(c, output);
                FileUtil.mkdirs(outFile.getParentFile());
                JSONArray paths = call.optJSONArray("paths");
                ArrayList<File> inputs = new ArrayList<File>();
                if (paths == null || paths.length() == 0) {
                    inputs.add(root(c));
                } else {
                    for (int i = 0; i < paths.length(); i++) {
                        inputs.add(resolve(c, paths.optString(i, "")));
                    }
                }
                int count = zip(c, inputs, outFile);
                changed.add(outFile.getAbsolutePath());
                out.put("ok", true);
                out.put("output", "已压缩 " + count + " 个条目 → " + relative(c, outFile)
                        + "（" + sizeText(outFile.length()) + "）");
            } else if (TOOL_UNZIP.equals(tool)) {
                File zipFile = resolve(c, path);
                if (!zipFile.exists() || !zipFile.isFile()) throw new IOException("压缩包不存在：" + path);
                String outDir = call.optString("output", "");
                File dir = outDir.length() == 0
                        ? new File(zipFile.getParentFile(), baseName(zipFile.getName()) + "_unzipped")
                        : resolve(c, outDir);
                FileUtil.mkdirs(dir);
                int count = unzip(zipFile, dir, c);
                changed.add(dir.getAbsolutePath());
                out.put("ok", true);
                out.put("output", "已解压 " + count + " 个文件 → " + relative(c, dir));
            } else if (TOOL_INFO.equals(tool)) {
                File f = resolve(c, path);
                out.put("ok", true);
                out.put("output", describe(c, f));
            } else {
                out.put("ok", false);
                out.put("output", "未知工具：" + tool);
            }
        } catch (Throwable t) {
            try {
                out.put("ok", false);
                out.put("output", t.getClass().getSimpleName() + "：" + t.getMessage());
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static String describe(Context c, File f) {
        StringBuilder sb = new StringBuilder();
        sb.append(relative(c, f));
        if (!f.exists()) {
            sb.append("\n不存在");
            return sb.toString();
        }
        sb.append(f.isDirectory() ? "\n类型：文件夹" : "\n类型：文件");
        if (f.isFile()) sb.append("\n大小：").append(sizeText(f.length()));
        sb.append("\n修改时间：").append(new java.util.Date(f.lastModified()).toString());
        return sb.toString();
    }

    private static boolean looksBinary(String s) {
        int n = Math.min(s.length(), 64);
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (ch == 0) return true;
        }
        return false;
    }

    public static String listTree(Context c, File dir, int depth, int maxDepth, int max) {
        StringBuilder sb = new StringBuilder();
        int[] count = new int[]{0};
        listInto(c, dir, depth, maxDepth, max, sb, count);
        return sb.toString();
    }

    private static void listInto(Context c, File dir, int depth, int maxDepth, int max,
                                 StringBuilder sb, int[] count) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        sortFiles(kids);
        for (int i = 0; i < kids.length; i++) {
            if (count[0] >= max) {
                sb.append("…（还有更多文件，未全部列出）\n");
                return;
            }
            File f = kids[i];
            for (int d = 0; d < depth; d++) sb.append("  ");
            sb.append(f.isDirectory() ? "[目录] " : "[文件] ");
            sb.append(f.getName());
            if (f.isFile()) sb.append("  (").append(sizeText(f.length())).append(")");
            sb.append('\n');
            count[0]++;
            if (f.isDirectory() && depth + 1 < maxDepth && count[0] < max) {
                listInto(c, f, depth + 1, maxDepth, max, sb, count);
            }
        }
    }

    private static void sortFiles(File[] files) {
        for (int i = 0; i < files.length; i++) {
            for (int j = i + 1; j < files.length; j++) {
                if (files[j].getName().compareToIgnoreCase(files[i].getName()) < 0) {
                    File t = files[i];
                    files[i] = files[j];
                    files[j] = t;
                }
            }
        }
    }

    /** 供分享/保存使用：把一个目录压成 zip。 */
    public static int zipFolder(Context c, File dir, File out) throws IOException {
        ArrayList<File> inputs = new ArrayList<File>();
        inputs.add(dir);
        return zip(c, inputs, out);
    }

    /** 返回写入的条目数。 */
    private static int zip(Context c, ArrayList<File> inputs, File out) throws IOException {
        File root = root(c);
        String outPath = out.getCanonicalPath();
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
        int count = 0;
        try {
            final int[] counter = new int[]{0};
            for (int i = 0; i < inputs.size(); i++) {
                File f = inputs.get(i);
                if (!f.exists()) continue;
                if (f.isFile()) {
                    if (f.getCanonicalPath().equals(outPath)) continue;
                    addZipEntry(zos, f, entryName(root, f), counter);
                    count++;
                } else {
                    count += zipDir(zos, root, f, outPath, counter);
                }
            }
        } finally {
            try {
                zos.close();
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    private static int zipDir(ZipOutputStream zos, File root, File dir, String outPath,
                              int[] counter) throws IOException {
        int count = 0;
        File[] kids = dir.listFiles();
        if (kids == null) return 0;
        if (kids.length == 0) {
            addZipEntry(zos, dir, entryName(root, dir) + "/", counter);
            return 1;
        }
        for (int i = 0; i < kids.length; i++) {
            File f = kids[i];
            if (f.getCanonicalPath().equals(outPath)) continue;
            if (f.isDirectory()) {
                count += zipDir(zos, root, f, outPath, counter);
            } else {
                addZipEntry(zos, f, entryName(root, f), counter);
                count++;
            }
        }
        return count;
    }

    private static String entryName(File root, File f) throws IOException {
        String rp = root.getCanonicalPath();
        String p = f.getCanonicalPath();
        if (p.startsWith(rp + File.separator)) return p.substring(rp.length() + 1).replace(File.separatorChar, '/');
        return f.getName();
    }

    private static void addZipEntry(ZipOutputStream zos, File f, String name, int[] counter)
            throws IOException {
        if (counter[0] > MAX_ZIP_ENTRIES) throw new IOException("条目过多，已停止压缩");
        counter[0]++;
        ZipEntry e = new ZipEntry(name);
        e.setTime(f.lastModified());
        zos.putNextEntry(e);
        if (f.isFile()) {
            FileInputStream in = new FileInputStream(f);
            try {
                FileUtil.copy(in, zos);
            } finally {
                FileUtil.close(in);
            }
        }
        zos.closeEntry();
    }

    private static int unzip(File zipFile, File dir, Context c) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        int count = 0;
        long total = 0;
        try {
            String rootPath = root(c).getCanonicalPath();
            String dirPath = dir.getCanonicalPath();
            if (!dirPath.equals(rootPath) && !dirPath.startsWith(rootPath + File.separator)) {
                throw new SecurityException("解压目录越界");
            }
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                count++;
                if (count > MAX_ZIP_ENTRIES) throw new IOException("压缩包条目过多");
                String name = e.getName();
                if (name == null || name.length() == 0) continue;
                File out = new File(dir, name);
                String p = out.getCanonicalPath();
                if (!p.equals(dirPath) && !p.startsWith(dirPath + File.separator)) {
                    throw new SecurityException("压缩包内含越界路径：" + name);
                }
                if (e.isDirectory()) {
                    FileUtil.mkdirs(out);
                    continue;
                }
                FileUtil.mkdirs(out.getParentFile());
                FileOutputStream fos = new FileOutputStream(out);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zis.read(buf)) > 0) {
                        total += n;
                        if (total > MAX_TOTAL_UNZIP) throw new IOException("解压体积超过 200 MB");
                        fos.write(buf, 0, n);
                    }
                } finally {
                    FileUtil.close(fos);
                }
            }
        } finally {
            try {
                zis.close();
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    private static void copyRecursive(File from, File to) throws IOException {
        if (from.isDirectory()) {
            FileUtil.mkdirs(to);
            File[] kids = from.listFiles();
            if (kids != null) {
                for (int i = 0; i < kids.length; i++) {
                    copyRecursive(kids[i], new File(to, kids[i].getName()));
                }
            }
        } else {
            FileInputStream in = new FileInputStream(from);
            FileOutputStream out = new FileOutputStream(to);
            try {
                FileUtil.copy(in, out);
            } finally {
                FileUtil.close(in);
                FileUtil.close(out);
            }
        }
    }

    public static String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public static String ext(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1).toLowerCase() : "";
    }

    public static String sizeText(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return oneDecimal(bytes / 1024.0) + " KB";
        if (bytes < 1024L * 1024L * 1024L) return oneDecimal(bytes / (1024.0 * 1024.0)) + " MB";
        return oneDecimal(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
    }

    private static String oneDecimal(double v) {
        long n = (long) (v * 10 + 0.5);
        return (n / 10) + "." + (n % 10);
    }

    /** 给系统提示词用的工具说明书。 */
    public static String protocol(Context c) {
        String rootDesc;
        if (Experiment.allFilesReady(c)) {
            rootDesc = "当前工作目录是设备存储根目录（/storage/emulated/0 或厂商等价路径），可以访问用户授权的全部文件。";
        } else {
            rootDesc = "当前工作目录是应用私有工作区（files/workspace），只能访问这里的文件。";
        }
        return "你现在处于「Agent 多轮任务」模式。你可以分多轮思考、执行工具、再思考，直到任务完成。\n"
                + rootDesc + "\n"
                + "如需调用工具，必须单独输出一行（可连续多行）：\n"
                + "<tool>{\"tool\":\"write_file\",\"path\":\"index.html\",\"content\":\"...\"}</tool>\n"
                + "可用工具：\n"
                + "- list_files：{\"path\":\".\"} 列出目录（最多 3 层）\n"
                + "- read_file：{\"path\":\"a.txt\"} 读取文本（最大 400 KB）\n"
                + "- write_file：{\"path\":\"a.html\",\"content\":\"...\"} 覆盖写入\n"
                + "- append_file：{\"path\":\"a.txt\",\"content\":\"...\"} 追加\n"
                + "- delete_file：{\"path\":\"a.txt\"} 删除文件或文件夹\n"
                + "- make_dir：{\"path\":\"pages\"} 创建文件夹\n"
                + "- move_file：{\"from\":\"a\",\"to\":\"b\"} 移动/重命名\n"
                + "- zip：{\"paths\":[\"pages\",\"index.html\"],\"output\":\"site.zip\"} 压缩\n"
                + "- unzip：{\"path\":\"site.zip\",\"output\":\"site\"} 解压（防目录穿越）\n"
                + "- file_info：{\"path\":\"a.txt\"} 查看大小与时间\n"
                + "规则：路径一律用相对工作目录的路径；禁止执行代码/命令；不得尝试跳出工作目录；"
                + "写 HTML 时把 CSS/JS 写成同目录文件并用相对路径引用，这样预览时能完整加载；"
                + "任务完成后不要再输出工具块，直接用自然语言给出简短总结和产物说明。\n";
    }
}
