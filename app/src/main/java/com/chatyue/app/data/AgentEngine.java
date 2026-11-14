package com.chatyue.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.chatyue.app.net.ChatClient;
import com.chatyue.app.net.Request;
import com.chatyue.app.net.RequestQueue;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 3.0 的多轮 Agent：思考 → 调用工具 → 看结果 → 继续思考，直到任务完成。
 *
 * 每一轮都是一次完整的 /chat/completions 请求；工具调用用 <tool>{json}</tool>
 * 这种最稳的纯文本协议，模型不会因为 function calling 的版本差异而失效。
 */
public final class AgentEngine {

    public interface Callback {
        /** 过程日志，用于气泡里的任务卡片。 */
        void onLog(String line);

        void onRound(int round);

        void onFinal(String text, String reasoning, ArrayList<String> artifacts);

        void onError(String message, String detail);

        void onStopped();
    }

    public static final class Parsed {
        String clean = "";
        ArrayList<JSONObject> calls = new ArrayList<JSONObject>();
        boolean invalid;
        String error = "";
    }

    private final Context ctx;
    private final ArrayList<Message> work;
    private final String baseSystem;
    private final Callback cb;
    private final int maxRounds;
    private final ArrayList<String> artifacts = new ArrayList<String>();
    private String lastClean = "";

    private Request request;
    private boolean stopped;
    private String lastReasoning = "";

    public AgentEngine(Context c, ArrayList<Message> history, String systemPrompt, Callback callback) {
        ctx = c;
        work = new ArrayList<Message>();
        for (int i = 0; i < history.size(); i++) {
            Message m = history.get(i);
            Message copy = new Message(m.role, m.text);
            copy.imagePath = m.imagePath;
            copy.attachments = m.attachments;
            work.add(copy);
        }
        baseSystem = systemPrompt == null ? "" : systemPrompt;
        cb = callback;
        maxRounds = Prefs.agentMaxRounds(c);
    }

    public void start() {
        round(1);
    }

    public void cancel() {
        stopped = true;
        if (request != null) request.cancel();
    }

    private void round(final int index) {
        if (stopped) {
            cb.onStopped();
            return;
        }
        cb.onRound(index);
        String sys = baseSystem;
        sys = sys + "\n\n" + AgentTools.protocol(ctx);
        String tree = AgentTools.listTree(ctx, AgentTools.root(ctx), 0, 1, 80);
        sys = sys + "\n工作目录现有文件：\n" + (tree.length() == 0 ? "（空）" : tree);

        final ArrayList<Message> history = new ArrayList<Message>(work);
        final StringBuffer content = new StringBuffer();
        final StringBuffer reasoning = new StringBuffer();

        request = ChatClient.send(ctx, Providers.current(ctx), history, sys,
                Prefs.getBool(ctx, Prefs.KEY_MEMORY, true), new ChatClient.Callback() {
                    public void onStart() {
                    }

                    public void onDelta(String c, String r) {
                        if (c != null) content.append(c);
                        if (r != null) reasoning.append(r);
                    }

                    public void onDone(final boolean wasStopped) {
                        RequestQueue.get().postMain(new Runnable() {
                            public void run() {
                                if (stopped || wasStopped) {
                                    cb.onStopped();
                                    return;
                                }
                                handleRound(index, content.toString(), reasoning.toString());
                            }
                        });
                    }

                    public void onError(final String message, final String detail, boolean tls) {
                        RequestQueue.get().postMain(new Runnable() {
                            public void run() {
                                if (stopped) cb.onStopped();
                                else cb.onError(message, detail);
                            }
                        });
                    }
                }, true);
    }

    private void handleRound(final int index, String raw, String reasoning) {
        lastReasoning = reasoning;
        final Parsed parsed = parse(raw);
        if (parsed.clean.length() > 0) lastClean = parsed.clean;
        if (parsed.calls.isEmpty()) {
            if (parsed.invalid && (maxRounds <= 0 || index < maxRounds)) {
                // 模型写了 <tool> 但 JSON 不合法：不要把它当最终回答，把错误回灌让它修正。
                cb.onLog("工具块格式错误，正在让模型修正：" + parsed.error);
                Message bad = new Message(Message.ROLE_USER,
                        "[系统提示] 你上一轮的 <tool> 块不是合法 JSON，未执行。" + parsed.error
                                + "\n请只输出合法格式：<tool>{\"tool\":\"...\",...}</tool>，"
                                + "或直接给最终回答。");
                work.add(bad);
                round(index + 1);
                return;
            }
            cb.onFinal(lastClean.length() == 0 ? "任务已完成。" : lastClean, lastReasoning, artifacts);
            return;
        }
        if (maxRounds > 0 && index >= maxRounds) {
            String note = "（已达到 " + maxRounds + " 轮上限，停止继续执行工具）";
            cb.onLog(note);
            cb.onFinal(lastClean.length() == 0
                    ? "已完成能做的部分，产物见下方卡片。" : lastClean, lastReasoning, artifacts);
            return;
        }

        Message assistant = new Message(Message.ROLE_ASSISTANT, raw);
        work.add(assistant);

        RequestQueue.get().submit(new Runnable() {
            public void run() {
                final StringBuffer results = new StringBuffer();
                final ArrayList<String> changed = new ArrayList<String>();
                for (int i = 0; i < parsed.calls.size(); i++) {
                    JSONObject call = parsed.calls.get(i);
                    String tool = call.optString("tool", call.optString("name", ""));
                    String path = call.optString("path", call.optString("output", ""));
                    JSONObject r = AgentTools.execute(ctx, call, changed);
                    String line = summarize(tool, path, r.optString("output", ""));
                    if (results.length() > 0) results.append('\n');
                    results.append(i + 1).append(". ").append(tool).append(" → ").append(line);
                }
                for (int i = 0; i < changed.size(); i++) {
                    addArtifact(changed.get(i));
                }
                Message toolMsg = new Message(Message.ROLE_USER,
                        "[工具执行结果，仅你可见]\n" + results.toString()
                                + "\n\n请根据结果继续。如果任务已完成，直接用自然语言总结，不要再输出 <tool> 块。");
                work.add(toolMsg);
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        cb.onLog(results.toString());
                        round(index + 1);
                    }
                });
            }
        });
    }

    private void addArtifact(String path) {
        if (path == null) return;
        try {
            java.io.File f = new java.io.File(path);
            if (!f.exists() || f.isDirectory() && f.listFiles() != null
                    && f.listFiles().length == 0) return;
            for (int i = 0; i < artifacts.size(); i++) {
                if (artifacts.get(i).equals(path)) return;
            }
            artifacts.add(path);
        } catch (Throwable ignored) {
        }
    }

    private static String summarize(String tool, String path, String output) {
        String s = output == null ? "" : output.replace('\n', ' ');
        if (s.length() > 160) s = s.substring(0, 160) + "…";
        if (s.length() == 0) s = "ok";
        return s;
    }

    /** 抽取所有 <tool> 块，剩下的文本才是给用户看的。兼容未闭合、数组、代码围栏。 */
    public static Parsed parse(String raw) {
        Parsed p = new Parsed();
        if (raw == null) return p;
        StringBuilder clean = new StringBuilder();
        int from = 0;
        while (from < raw.length()) {
            int start = findToolOpen(raw, from);
            if (start < 0) {
                clean.append(raw.substring(from));
                break;
            }
            clean.append(raw.substring(from, start));
            int bodyStart = raw.indexOf('>', start);
            if (bodyStart < 0) {
                p.invalid = true;
                p.error = " <tool> 标签未闭合";
                break;
            }
            String closeTag = raw.startsWith("<tool_call>", start) ? "</tool_call>" : "</tool>";
            int end = raw.indexOf(closeTag, bodyStart + 1);
            String body;
            if (end < 0) {
                // 流式模型经常漏掉闭合标签：把剩余内容当工具体，避免把标签当最终回答。
                body = raw.substring(bodyStart + 1);
                from = raw.length();
                p.invalid = false;
            } else {
                body = raw.substring(bodyStart + 1, end);
                from = end + closeTag.length();
            }
            String trimmed = stripFence(body);
            int added = 0;
            try {
                JSONObject one = new JSONObject(trimmed);
                p.calls.add(one);
                added = 1;
            } catch (Throwable ignored) {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(trimmed);
                    for (int i = 0; i < arr.length(); i++) {
                        Object o = arr.opt(i);
                        if (o instanceof JSONObject) {
                            p.calls.add((JSONObject) o);
                            added++;
                        }
                    }
                } catch (Throwable ignored2) {
                }
            }
            if (added == 0) {
                // 尝试从任意位置截取第一个 JSON 对象，兼容“说明文字 + JSON”。
                int ob = trimmed.indexOf('{');
                int cb = trimmed.lastIndexOf('}');
                if (ob >= 0 && cb > ob) {
                    try {
                        p.calls.add(new JSONObject(trimmed.substring(ob, cb + 1)));
                        added = 1;
                    } catch (Throwable ignored3) {
                    }
                }
            }
            if (added == 0) {
                p.invalid = true;
                p.error = "工具块不是合法 JSON：" + clip(trimmed);
            }
            if (end < 0) break;
        }
        p.clean = clean.toString().trim();
        return p;
    }

    private static int findToolOpen(String raw, int from) {
        int a = raw.indexOf("<tool>", from);
        int b = raw.indexOf("<tool_call>", from);
        if (a < 0) return b;
        if (b < 0) return a;
        return Math.min(a, b);
    }

    private static String stripFence(String body) {
        String s = body == null ? "" : body.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) s = s.substring(nl + 1).trim();
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3).trim();
        }
        return s;
    }

    private static String clip(String s) {
        if (s == null) return "";
        String x = s.replace('\n', ' ');
        return x.length() > 120 ? x.substring(0, 120) + "…" : x;
    }
}
