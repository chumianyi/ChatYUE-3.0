package com.chatyue.app.net;

import android.content.Context;

import com.chatyue.app.data.Message;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Provider;
import com.chatyue.app.util.Base64Codec;
import com.chatyue.app.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * OpenAI 兼容的对话客户端。
 *
 * 关键点：
 * 1. 请求体按 OpenAI /chat/completions 规范拼装，支持 vision（image_url + base64 data URL）。
 * 2. 响应必须是 SSE，逐行解析 data: 块，边解析边回调，实现逐字输出。
 * 3. 平台 TLS 失败时自动切 wolfSSL 链路；两条链路都失败才报错，并给出可读原因。
 */
public final class ChatClient {

    public interface Callback {
        void onStart();

        void onDelta(String content, String reasoning);

        void onDone(boolean stopped);

        void onError(String message, String detail, boolean tlsProblem);
    }

    private ChatClient() {
    }

    public static Request send(final Context ctx, final Provider provider, final ArrayList<Message> history,
                               final String systemPrompt, final boolean memory, final Callback cb) {
        return send(ctx, provider, history, systemPrompt, memory, cb, false);
    }

    public static Request send(final Context ctx, final Provider provider, final ArrayList<Message> history,
                               final String systemPrompt, final boolean memory, final Callback cb,
                               final boolean fullHistory) {
        final Request req = new Request();
        final String url = provider.chatUrl();
        final byte[] body;
        try {
            body = buildBody(ctx, provider, history, systemPrompt, memory,
                    Prefs.thinkingEnabled(ctx, provider), fullHistory);
        } catch (Exception e) {
            cb.onError("请求构造失败", e.getMessage(), false);
            return req;
        }
        final String apiKey = provider.apiKey;
        RequestQueue.get().submit(new Runnable() {
            public void run() {
                stream(url, apiKey, body, req, cb);
            }
        });
        return req;
    }

    private static void stream(String url, String apiKey, byte[] body, Request req, Callback cb) {
        Request.Conn conn = null;
        boolean usedWolf = false;
        try {
            try {
                conn = HttpTransport.open(url, apiKey, body, req);
            } catch (IOException e) {
                boolean https = url != null && url.toLowerCase().startsWith("https");
                if (https && Tls.isTlsError(e) && WolfSsl.isAvailable()) {
                    // 平台 TLS 握手失败 → 走 wolfSSL 兜底
                    conn = WolfTransport.open(url, apiKey, body, req);
                    usedWolf = true;
                } else {
                    throw e;
                }
            }
            if (conn.code >= 400) {
                String detail = drain(conn.reader, 1600);
                String msg = parseHttpError(conn.code, detail);
                cb.onError(msg, detail, false);
                return;
            }
            String ct = conn.contentType == null ? "" : conn.contentType.toLowerCase();
            if (ct.indexOf("application/json") >= 0 && ct.indexOf("event-stream") < 0) {
                String all = drain(conn.reader, 8 * 1024 * 1024);
                parseJsonResponse(all, cb, req);
                return;
            }
            parseSse(conn, cb, req);
        } catch (Exception e) {
            if (req.isCancelled()) {
                cb.onDone(true);
                return;
            }
            String msg = Tls.describe(e);
            if (Tls.isTlsError(e) && !usedWolf) {
                msg = "TLS 握手失败：本机 TLS 栈与证书库过旧，且 wolfSSL 兜底未生效（"
                        + (WolfSsl.isAvailable() ? "已加载但握手仍失败" : "libwolfssl_bridge.so 未打包") + "）";
            }
            cb.onError(msg, stack(e), Tls.isTlsError(e));
        } finally {
            if (conn != null) conn.closeQuietly();
        }
    }

    private static void parseSse(Request.Conn conn, Callback cb, Request req) throws IOException {
        BufferedReader reader = conn.reader;
        boolean started = false;
        String line;
        int guard = 0;
        while ((line = reader.readLine()) != null) {
            if (req.isCancelled()) {
                cb.onDone(true);
                return;
            }
            if (guard++ > 200000) break;
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.startsWith(":")) continue;
            if (line.startsWith("event:")) continue;
            if (!line.startsWith("data:")) continue;
            String payload = line.substring(5).trim();
            if (payload.length() == 0) continue;
            if ("[DONE]".equals(payload)) {
                cb.onDone(false);
                return;
            }
            if (!started) {
                started = true;
                cb.onStart();
            }
            try {
                JSONObject o = new JSONObject(payload);
                JSONObject err = o.optJSONObject("error");
                if (err != null) {
                    cb.onError(err.optString("message", "服务端返回错误"), payload, false);
                    return;
                }
                JSONArray choices = o.optJSONArray("choices");
                if (choices == null || choices.length() == 0) continue;
                JSONObject ch = choices.optJSONObject(0);
                if (ch == null) continue;
                JSONObject delta = ch.optJSONObject("delta");
                if (delta == null) delta = ch.optJSONObject("message");
                if (delta != null) {
                    String content = optText(delta, "content");
                    String reasoning = optText(delta, "reasoning_content");
                    if (reasoning.length() == 0) reasoning = optText(delta, "reasoning");
                    if (content.length() > 0 || reasoning.length() > 0) {
                        cb.onDelta(content, reasoning);
                    }
                }
            } catch (Exception pe) {
                // 单个分片解析失败不应中断整段输出
            }
        }
        cb.onDone(false);
    }

    /** 整数百分比 → 两位小数，避免 float 升 double 时带出长尾。 */
    private static double round2(int percent) {
        int v = percent < 0 ? 0 : percent;
        return Math.round(v / 100.0 * 100.0) / 100.0;
    }

    private static String optText(JSONObject o, String key) {        Object v = o.opt(key);
        if (v == null || v == JSONObject.NULL) return "";
        if (v instanceof String) return (String) v;
        return String.valueOf(v);
    }

    private static void parseJsonResponse(String raw, Callback cb, Request req) {
        try {
            JSONObject o = new JSONObject(raw);
            JSONObject err = o.optJSONObject("error");
            if (err != null) {
                cb.onError(err.optString("message", "服务端返回错误"), raw, false);
                return;
            }
            JSONArray choices = o.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                cb.onError("响应为空", raw, false);
                return;
            }
            JSONObject msg = choices.optJSONObject(0).optJSONObject("message");
            String content = msg == null ? "" : optText(msg, "content");
            String reasoning = msg == null ? "" : optText(msg, "reasoning_content");
            cb.onStart();
            if (reasoning.length() > 0) cb.onDelta("", reasoning);
            if (content.length() > 0) cb.onDelta(content, "");
            cb.onDone(req.isCancelled());
        } catch (Exception e) {
            cb.onError("响应解析失败", raw, false);
        }
    }

    private static String drain(BufferedReader r, int limit) {
        StringBuilder sb = new StringBuilder();
        try {
            char[] buf = new char[1024];
            int n;
            while ((n = r.read(buf)) > 0 && sb.length() < limit) {
                sb.append(buf, 0, n);
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private static String parseHttpError(int code, String detail) {
        String msg = "";
        try {
            JSONObject o = new JSONObject(detail);
            JSONObject err = o.optJSONObject("error");
            if (err != null) msg = err.optString("message", "");
            if (msg.length() == 0) msg = o.optString("message", "");
        } catch (Exception ignored) {
        }
        if (msg.length() == 0) {
            if (code == 401) msg = "API Key 无效或已过期";
            else if (code == 404) msg = "接口地址不存在，请检查 Base URL";
            else if (code == 429) msg = "请求过于频繁，请稍后再试";
            else if (code >= 500) msg = "服务端异常";
            else msg = detail.length() > 160 ? detail.substring(0, 160) : detail;
        }
        return "HTTP " + code + " · " + msg;
    }

    private static String stack(Throwable t) {
        if (t == null) return "";
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    private static byte[] buildBody(Context ctx, Provider p, ArrayList<Message> history,
                                    String systemPrompt, boolean memory) throws Exception {
        return buildBody(ctx, p, history, systemPrompt, memory, true);
    }

    private static byte[] buildBody(Context ctx, Provider p, ArrayList<Message> history,
                                    String systemPrompt, boolean memory, boolean thinking) throws Exception {
        return buildBody(ctx, p, history, systemPrompt, memory, thinking, false);
    }

    private static byte[] buildBody(Context ctx, Provider p, ArrayList<Message> history,
                                    String systemPrompt, boolean memory, boolean thinking,
                                    boolean fullHistory) throws Exception {
        JSONObject root = new JSONObject();
        root.put("model", p.model == null || p.model.length() == 0 ? "gpt-3.5-turbo" : p.model);
        root.put("stream", true);
        /*
         * 1210 错误的根因：Prefs 里温度是整数百分比，除以 100f 得到 float，
         * JSONObject.put 会把它升成 double，0.73f 变成 0.7300000190734863，
         * 服务端校验「小数点后最多 2 位」就会直接拒绝。
         * 这里统一收敛为两位小数的 double，0.73 序列化出来就是 0.73。
         */
        root.put("temperature", round2(ctx == null ? 70 : Prefs.temperature(ctx)));
        root.put("top_p", round2(ctx == null ? 90 : Prefs.topP(ctx)));
        root.put("max_tokens", ctx == null ? 2048 : Prefs.maxTokens(ctx));

        // 内置模型固定思考；第三方模型可以在这里显式关闭（两种最常见的服务端参数同时给出，
        // 不支持的服务端通常会忽略未知字段）。
        if (!thinking && p != null && !p.isBuiltin()) {
            JSONObject thinkingObj = new JSONObject();
            thinkingObj.put("type", "disabled");
            root.put("thinking", thinkingObj);
            root.put("enable_thinking", false);
        }

        JSONArray msgs = new JSONArray();
        if (systemPrompt != null && systemPrompt.trim().length() > 0) {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt.trim());
            msgs.put(sys);
        }

        ArrayList<Message> use = new ArrayList<Message>();
        if (fullHistory) {
            // Agent 的每一轮都要看到完整任务链（含工具结果），不能按普通对话截断。
            int start = Math.max(0, history.size() - 40);
            for (int i = start; i < history.size(); i++) use.add(history.get(i));
        } else if (memory) {
            int rounds = com.chatyue.app.data.Prefs.contextRounds(ctx) * 2;
            int start = Math.max(0, history.size() - rounds);
            for (int i = start; i < history.size(); i++) use.add(history.get(i));
        } else {
            for (int i = history.size() - 1; i >= 0; i--) {
                if (history.get(i).isUser()) {
                    use.add(history.get(i));
                    break;
                }
            }
        }

        for (int i = 0; i < use.size(); i++) {
            Message m = use.get(i);
            if (m.text == null || m.text.trim().length() == 0 && m.imagePath == null) continue;
            if (!m.isUser() && (m.text == null || m.text.trim().length() == 0)) continue;
            JSONObject o = new JSONObject();
            o.put("role", m.isUser() ? "user" : "assistant");
            String base64 = null;
            if (m.isUser() && m.imagePath != null) {
                base64 = readImage(ctx, m.imagePath);
            }
            if (base64 != null) {
                JSONArray content = new JSONArray();
                JSONObject t = new JSONObject();
                t.put("type", "text");
                t.put("text", m.text == null ? "" : m.text);
                content.put(t);
                JSONObject img = new JSONObject();
                img.put("type", "image_url");
                JSONObject urlObj = new JSONObject();
                urlObj.put("url", "data:image/jpeg;base64," + base64);
                img.put("image_url", urlObj);
                content.put(img);
                o.put("content", content);
            } else {
                o.put("content", m.text == null ? "" : m.text);
            }
            msgs.put(o);
        }
        root.put("messages", msgs);
        return root.toString().getBytes("UTF-8");
    }

    private static String readImage(Context ctx, String path) {
        FileInputStream in = null;
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            in = new FileInputStream(f);
            byte[] data = FileUtil.readAll(in);
            if (data.length > 3 * 1024 * 1024) return null;
            return Base64Codec.encode(data);
        } catch (Exception e) {
            return null;
        } finally {
            FileUtil.close(in);
        }
    }

    /** 供「测试连接」使用：发一条极短请求，只判断是否可达。 */
    public static void ping(final Provider p, final PingCallback cb) {
        final Request req = new Request();
        RequestQueue.get().submit(new Runnable() {
            public void run() {
                try {
                    ArrayList<Message> probe = new ArrayList<Message>();
                    probe.add(new Message(Message.ROLE_USER, "ping"));
                    byte[] body = buildBody(null, p, probe, "只回复 ok", true);
                    Request.Conn conn = HttpTransport.open(p.chatUrl(), p.apiKey, body, req);
                    if (conn.code >= 400) {
                        cb.onResult(false, parseHttpError(conn.code, drain(conn.reader, 800)));
                    } else {
                        cb.onResult(true, "连接正常（HTTP " + conn.code + "）");
                    }
                    conn.closeQuietly();
                } catch (Exception e) {
                    cb.onResult(false, Tls.describe(e));
                }
            }
        });
    }

    public interface PingCallback {
        void onResult(boolean ok, String message);
    }
}
