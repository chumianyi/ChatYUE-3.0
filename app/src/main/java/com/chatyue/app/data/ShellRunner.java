package com.chatyue.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Android 系统自带 shell 执行器。
 *
 * 终端页和 Agent 的 run_command 工具共用这一份实现：
 * - 固定使用 /system/bin/sh，不依赖 Termux；
 * - 工作目录默认是 Agent 工作目录；
 * - 64KB 输出上限、默认 15 秒超时，超时会 destroy 进程；
 * - 只拦截明显会毁坏系统/数据的命令，不做 root。
 */
public final class ShellRunner {

    public static final int DEFAULT_TIMEOUT = 15000;
    public static final int MAX_OUTPUT = 64 * 1024;

    public static final class Result {
        public final int exit;
        public final String output;
        public final boolean timeout;

        Result(int e, String o, boolean t) {
            exit = e;
            output = o == null ? "" : o;
            timeout = t;
        }

        public boolean ok() {
            return !timeout && exit == 0;
        }
    }

    private ShellRunner() {
    }

    public static String shellPath() {
        File f = new File("/system/bin/sh");
        return f.exists() ? f.getAbsolutePath() : "sh";
    }

    public static Result run(Context c, String command, File cwd, int timeoutMs) {
        if (command == null || command.trim().length() == 0) {
            return new Result(2, "命令为空", false);
        }
        if (isDangerous(command)) {
            return new Result(126, "已阻止高风险命令（不会执行 rm -rf /、mkfs、dd、reboot 等）", false);
        }
        Process process = null;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final boolean[] truncated = new boolean[]{false};
        try {
            File dir = cwd;
            if (dir == null || !dir.exists() || !dir.isDirectory()) dir = AgentTools.root(c);
            ProcessBuilder pb = new ProcessBuilder(shellPath(), "-c", command);
            pb.directory(dir);
            pb.redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("HOME", dir.getAbsolutePath());
            env.put("PWD", dir.getAbsolutePath());
            String path = env.get("PATH");
            if (path == null || path.length() == 0) path = "/system/bin:/system/xbin";
            if (path.indexOf("/data/data/com.termux/files/usr/bin") < 0) {
                path = path + ":/data/data/com.termux/files/usr/bin";
            }
            env.put("PATH", path);
            if (!env.containsKey("LD_LIBRARY_PATH")
                    && new File("/data/data/com.termux/files/usr/lib").exists()) {
                env.put("LD_LIBRARY_PATH", "/data/data/com.termux/files/usr/lib");
            }
            process = pb.start();

            final InputStream in = process.getInputStream();
            Thread reader = new Thread(new Runnable() {
                public void run() {
                    byte[] buf = new byte[4096];
                    int n;
                    try {
                        while ((n = in.read(buf)) > 0) {
                            if (buffer.size() < MAX_OUTPUT) {
                                int room = MAX_OUTPUT - buffer.size();
                                buffer.write(buf, 0, Math.min(n, room));
                                if (n > room) truncated[0] = true;
                            } else {
                                truncated[0] = true;
                            }
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        try {
                            in.close();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }, "chatyue-shell-reader");
            reader.start();

            final Process fp = process;
            final boolean[] timedOut = new boolean[]{false};
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable killer = new Runnable() {
                public void run() {
                    timedOut[0] = true;
                    try {
                        fp.destroy();
                    } catch (Throwable ignored) {
                    }
                }
            };
            handler.postDelayed(killer, Math.max(1000, timeoutMs));

            int exit;
            try {
                exit = process.waitFor();
            } catch (Throwable t) {
                exit = -1;
            }
            handler.removeCallbacks(killer);
            try {
                reader.join(2000);
            } catch (Throwable ignored) {
            }

            String text = buffer.toString("UTF-8");
            if (truncated[0]) text = text + "\n…（输出超过 64 KB，已截断）";
            if (timedOut[0]) text = text + "\n…（命令超过 " + (timeoutMs / 1000) + " 秒，已终止）";
            return new Result(exit, text, timedOut[0]);
        } catch (Throwable t) {
            return new Result(-1, t.getClass().getSimpleName() + "：" + t.getMessage(), false);
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 只拦截明显会毁坏设备的命令；普通开发命令全部放行。 */
    private static boolean isDangerous(String command) {
        String s = command.toLowerCase();
        String[] blocked = new String[]{
                "rm -rf /", "rm -rf /*", "mkfs", "dd if=", "reboot", "shutdown",
                "pm uninstall", "pm clear", "su ", "magisk"
        };
        for (int i = 0; i < blocked.length; i++) {
            if (s.indexOf(blocked[i]) >= 0) return true;
        }
        return false;
    }

    /** 探测系统 shell / Termux 里可用的 python，不强行内置解释器。 */
    public static String pythonPath(Context c) {
        String[] candidates = new String[]{
                "/system/bin/python3", "/system/xbin/python3",
                "/data/data/com.termux/files/usr/bin/python3",
                "/data/data/com.termux/files/usr/bin/python"
        };
        for (int i = 0; i < candidates.length; i++) {
            File f = new File(candidates[i]);
            if (f.exists() && f.canExecute()) return f.getAbsolutePath();
        }
        try {
            Result r = run(c, "command -v python3 || command -v python", AgentTools.root(c), 4000);
            String out = r.output.trim();
            if (out.length() > 0 && !out.contains("not found") && !out.contains("No such")) {
                int nl = out.indexOf('\n');
                if (nl >= 0) out = out.substring(0, nl).trim();
                if (out.startsWith("/")) return out;
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    public static String toolSummary(Context c) {
        StringBuilder sb = new StringBuilder();
        sb.append("系统 shell：").append(shellPath()).append('\n');
        sb.append("工作目录：").append(AgentTools.root(c).getAbsolutePath()).append('\n');
        sb.append("常用命令：ls、cat、mkdir、mv、cp、rm、grep、sed、awk、find、zip/unzip\n");
        String py = pythonPath(c);
        if (py.length() > 0) sb.append("Python：").append(py);
        else sb.append("Python：未检测到（系统通常不自带 Python；安装 Termux 后会自动识别 /data/data/com.termux/files/usr/bin/python3）");
        return sb.toString();
    }
}
