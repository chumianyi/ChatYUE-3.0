package com.chatyue.app.data;

import android.content.Context;

import com.chatyue.app.net.ChatClient;
import com.chatyue.app.net.Request;
import com.chatyue.app.net.RequestQueue;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 实验性「反思」：
 * 回复完成后（或用户指出错误时）额外跑一次思维，检查刚才哪里做错了；
 * 如果确实有错，先诚恳认错，再给出修复后的完整答案。
 */
public final class ReflectionEngine {

    public interface Callback {
        void onResult(boolean hasError, String apology, String fixed, String reasoning);

        void onError(String message);
    }

    private ReflectionEngine() {
    }

    /** 本地启发式：出现这些信号才自动反思，避免每条回复都多花一次请求。 */
    public static boolean looksWrong(String text) {
        if (text == null || text.trim().length() == 0) return true;
        String t = text;
        String[] signals = {
                "我无法", "无法完成", "抱歉，我", "我搞错", "刚才说错", "出错",
                "执行失败", "操作失败", "不能提供", "不存在该"
        };
        for (int i = 0; i < signals.length; i++) {
            if (t.indexOf(signals[i]) >= 0) return true;
        }
        return false;
    }

    public static boolean looksLikeCorrection(String userText) {
        if (userText == null) return false;
        String t = userText.trim();
        String[] signals = {"不对", "错了", "不是这样", "搞错", "有误", "重新来", "你确定"};
        for (int i = 0; i < signals.length; i++) {
            if (t.indexOf(signals[i]) >= 0) return true;
        }
        return false;
    }

    public static Request run(Context c, ArrayList<Message> history, Message answer,
                              final boolean manual, final Callback cb) {
        ArrayList<Message> work = new ArrayList<Message>();
        for (int i = 0; i < history.size(); i++) {
            Message m = history.get(i);
            if (m == answer) continue;
            if (m.streaming || m.error) continue;
            if (m.text == null || m.text.trim().length() == 0) continue;
            Message copy = new Message(m.role, m.text);
            work.add(copy);
        }
        if (answer != null && answer.text != null && answer.text.trim().length() > 0) {
            work.add(new Message(Message.ROLE_ASSISTANT, answer.text));
        }
        Message task = new Message(Message.ROLE_USER,
                "请严格审查你刚才那条回复（如下）。只输出一个 JSON 对象，不要输出别的文字：\n"
                        + "{\"has_error\":true或false,\"reason\":\"错在哪\","
                        + "\"apology\":\"如果做错了，用第一人称向用户诚恳认错的一句话；没错则为空\","
                        + "\"fixed\":\"如果做错了，修复后的完整答案；没错则为空\"}\n\n"
                        + "被审查的回复：\n<<<\n" + (answer == null || answer.text == null ? "" : answer.text)
                        + "\n>>>\n\n要求：只有确实存在事实错误、逻辑错误、答非所问或没有完成任务时才判 has_error=true；"
                        + "不要为了显得认真而虚构错误。");
        final String sys = "你是一个严谨的校对者。你的任务是审查自己上一条回复是否真的有错，"
                + "有错就认错并给出修复后的完整答案，没错就如实说明。";
        final StringBuffer content = new StringBuffer();

        Request req = ChatClient.send(c, Providers.current(c), work, sys, true,
                new ChatClient.Callback() {
                    public void onStart() {
                    }

                    public void onDelta(String d, String r) {
                        if (d != null) content.append(d);
                    }

                    public void onDone(boolean stopped) {
                        final String raw = content.toString();
                        RequestQueue.get().postMain(new Runnable() {
                            public void run() {
                                parse(raw, manual, cb);
                            }
                        });
                    }

                    public void onError(final String message, final String detail, boolean tls) {
                        RequestQueue.get().postMain(new Runnable() {
                            public void run() {
                                cb.onError(message);
                            }
                        });
                    }
                }, true);
        return req;
    }

    private static void parse(String raw, boolean manual, Callback cb) {
        String text = raw == null ? "" : raw.trim();
        try {
            int a = text.indexOf('{');
            int b = text.lastIndexOf('}');
            if (a >= 0 && b > a) {
                JSONObject o = new JSONObject(text.substring(a, b + 1));
                boolean has = o.optBoolean("has_error", false);
                String apology = o.optString("apology", "");
                String fixed = o.optString("fixed", "");
                String reason = o.optString("reason", "");
                if (has && fixed.trim().length() > 0) {
                    if (apology.length() == 0) apology = "抱歉，刚才这条回复有问题，我重新修正。";
                    cb.onResult(true, apology, fixed, reason);
                    return;
                }
                if (manual) {
                    cb.onResult(false, reason.length() == 0 ? "复查过了，这条回复没有发现明显错误。" : reason,
                            "", "");
                    return;
                }
                cb.onResult(false, "", "", "");
                return;
            }
        } catch (Throwable ignored) {
        }
        // 模型没按 JSON 输出时，把它的复查结论原样展示（仅手动触发才显示）
        if (manual && text.length() > 0) {
            cb.onResult(false, text, "", "");
        } else {
            cb.onResult(false, "", "", "");
        }
    }
}
