package com.chatyue.app.ui.m3;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Style;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.AgentEngine;
import com.chatyue.app.data.AgentFlags;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.data.Attachments;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.Lang;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Profile;
import com.chatyue.app.data.Provider;
import com.chatyue.app.data.Providers;
import com.chatyue.app.data.ReflectionEngine;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.data.Skills;
import com.chatyue.app.net.ChatClient;
import com.chatyue.app.net.Request;
import com.chatyue.app.net.RequestQueue;
import com.chatyue.app.net.WolfSsl;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.Avatar;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MorphFrame;
import com.chatyue.app.ui.widget.PhotoView;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.util.TimeUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * 经典主题对话页。流式逻辑与默认 ChatPage 相同，但界面全部换成
 * M3 的大顶栏、无气泡正文、胶囊输入条与统一底部面板。
 */
public class M3ChatPage extends Page implements M3InputBar.Listener {

    private final Session session;
    private final Object lock = new Object();

    private M3TopBar topBar;
    private BounceScrollView scroll;
    private LinearLayout transcript;
    private M3InputBar input;
    private LinearLayout quickBar;
    private RoundBox attachChip;

    private M3MessageView streamingView;
    private Message streamingMsg;
    private Request request;
    private boolean streaming;
    private String pendingContent = "";
    private String pendingReasoning = "";
    private boolean flushScheduled;
    private String attachedImage;
    private final ArrayList<String> attachments = new ArrayList<String>();
    private AgentEngine agent;
    private boolean agentFinished;
    private boolean reflecting;
    private int dotPhase;
    private boolean tickerRunning;

    private final Runnable flushTask = new Runnable() {
        public void run() {
            synchronized (lock) {
                flushScheduled = false;
            }
            applyPending();
        }
    };

    private final Runnable subtitleTicker = new Runnable() {
        public void run() {
            if (!tickerRunning) return;
            dotPhase = (dotPhase + 1) % 4;
            topBar.setSubtitle("正在回复" + dots());
            Tween.post(this, 280);
        }
    };

    public M3ChatPage(MainActivity a, Session s) {
        super(a);
        session = s != null ? s : SessionStore.create(a);
        setBackgroundColor(M3.BG);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        topBar = new M3TopBar(a, session.title, subtitle(), Icons.BACK, Icons.MORE);
        topBar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        if (topBar.right != null) {
            topBar.right.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openMoreMenu();
                }
            });
        }
        root.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(70)));

        scroll = new BounceScrollView(a);
        transcript = new LinearLayout(a);
        transcript.setOrientation(LinearLayout.VERTICAL);
        transcript.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(16));
        scroll.addView(transcript, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        quickBar = buildQuickBar();
        root.addView(quickBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(44)));

        LinearLayout inputWrap = new LinearLayout(a);
        inputWrap.setOrientation(LinearLayout.VERTICAL);
        inputWrap.setPadding(Ui.dp(12), Ui.dp(2), Ui.dp(12), Ui.dp(8));

        attachChip = new RoundBox(a);
        attachChip.setFill(M3.PRIMARY_CONTAINER);
        attachChip.setRadius(16);
        attachChip.setPadding(Ui.dp(10), Ui.dp(6), Ui.dp(8), Ui.dp(6));
        attachChip.setVisibility(GONE);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        chipLp.bottomMargin = Ui.dp(8);
        inputWrap.addView(attachChip, chipLp);

        input = new M3InputBar(a);
        input.setListener(this);
        inputWrap.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(inputWrap, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        renderMessages(false);
        updateQuickBar();
    }

    private String dots() {
        if (dotPhase == 0) return "";
        if (dotPhase == 1) return ".";
        if (dotPhase == 2) return "..";
        return "...";
    }

    private String subtitle() {
        int days = TimeUtil.daysSince(Prefs.firstLaunch(act));
        String server = Prefs.serverEnabled(act) && Prefs.serverConfigured(act)
                ? " · 服务器已连接" : "";
        return Providers.current(act).name + " · 陪伴 " + days + " 天" + server;
    }

    private LinearLayout buildQuickBar() {
        LinearLayout bar = new LinearLayout(act);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
        bar.addView(quickChip(Icons.COPY, "复制", new Runnable() {
            public void run() {
                String text = lastAssistantText();
                if (text.length() == 0) act.toast("还没有可复制的内容");
                else act.copyText(text);
            }
        }), chipLp(0));
        bar.addView(quickChip(Icons.REFRESH, "重试", new Runnable() {
            public void run() {
                regenerate();
            }
        }), chipLp(7));
        bar.addView(quickChip(Icons.TRASH, "清空", new Runnable() {
            public void run() {
                confirmClear();
            }
        }), chipLp(7));
        return bar;
    }

    private View quickChip(int icon, String text, final Runnable action) {
        M3Button b = new M3Button(act, text, M3Button.TONAL);
        b.icon(icon).height(34).padH(13);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                action.run();
            }
        });
        return b;
    }

    private LinearLayout.LayoutParams chipLp(int left) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(34));
        lp.leftMargin = Ui.dp(left);
        return lp;
    }

    // ------------------------------------------------------------ 消息渲染

    private void renderMessages(boolean animateNew) {
        transcript.removeAllViews();
        streamingView = null;
        if (session.messages.isEmpty()) {
            transcript.addView(buildWelcome(), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }
        for (int i = 0; i < session.messages.size(); i++) {
            Message m = session.messages.get(i);
            final M3MessageView view = new M3MessageView(act, m);
            view.setGestureListener(new M3MessageView.GestureListener() {
                public void onLongPress(M3MessageView v) {
                    // 兼容旧接口；实际入口是气泡右上角三点。
                }

                public void onMenu(M3MessageView v) {
                    M3ChatPage.this.onLongPress(v.getMessage());
                }

                public void onDoubleTap(M3MessageView v) {
                    M3ChatPage.this.onDoubleTap(v.getMessage());
                }
            });
            final MorphFrame wrap = new MorphFrame(act);
            wrap.pivot(0.5f, 0f);
            wrap.addView(view, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(i == 0 ? 8 : 16);
            transcript.addView(wrap, lp);
            if (m.streaming) {
                streamingView = view;
                streamingMsg = m;
            }
            if (animateNew && i >= session.messages.size() - 2) {
                final int delay = (i - (session.messages.size() - 2)) * 45;
                wrap.transform(0f, 0.97f, 0f, Ui.dp(12));
                Tween t = Tween.of(0, 1, 230, new Tween.Listener() {
                    public void onUpdate(float v) {
                        wrap.transform(v, 0.97f + 0.03f * v, 0f, (int) (Ui.dp(12) * (1f - v)));
                    }

                    public void onEnd() {
                        wrap.transform(1f, 1f, 0f, 0f);
                    }
                }).delay(delay);
                t.ease(Tween.DECEL).start();
            }
        }
        scrollToBottom();
        updateQuickBar();
    }

    private View buildWelcome() {
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(0, Ui.dp(44), 0, 0);

        Avatar av = new Avatar(act);
        av.setLetter("C");
        av.setColors(M3.PRIMARY_CONTAINER, M3.PRIMARY_DEEP);
        col.addView(av, new LinearLayout.LayoutParams(Ui.dp(76), Ui.dp(76)));

        Label hi = new Label(act, "开始一段新对话", 24f, M3.ON_SURFACE, true);
        hi.setAlign(Label.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(18);
        col.addView(hi, hlp);

        Label tip = new Label(act, "文字、图片、代码都可以，想到什么直接说。", 12.6f,
                M3.ON_SURFACE_3, false);
        tip.setAlign(Label.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(7);
        col.addView(tip, tlp);

        LinearLayout suggest = new LinearLayout(act);
        suggest.setOrientation(LinearLayout.HORIZONTAL);
        addSuggest(suggest, "写代码", "帮我写一个 Android 原生小部件：", 0);
        addSuggest(suggest, "翻译", "把下面内容翻译成自然地道的英文：\n", 1);
        addSuggest(suggest, "总结", "把下面的内容整理成三条要点：\n", 2);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(38));
        slp.topMargin = Ui.dp(22);
        col.addView(suggest, slp);

        return col;
    }

    private void addSuggest(LinearLayout row, String text, final String preset, int index) {
        M3Button b = new M3Button(act, text, M3Button.TONAL);
        b.height(38).padH(8);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                input.setText(preset);
                input.field().requestFocus();
                showKeyboard();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(38), 1f);
        if (index > 0) lp.leftMargin = Ui.dp(8);
        row.addView(b, lp);
    }

    private void scrollToBottom() {
        scroll.scrollToBottomSoon();
        RequestQueue.get().postMainDelayed(new Runnable() {
            public void run() {
                scroll.scrollToBottomNow();
            }
        }, 60);
    }

    // ------------------------------------------------------------ 发送与流式

    public void setDraft(final String draft) {
        input.setText(draft);
        input.post(new Runnable() {
            public void run() {
                input.field().requestFocus();
                input.field().setSelection(input.getText().length());
                showKeyboard();
            }
        });
    }

    public void onSend(String text) {
        if (streaming) {
            act.toast("正在生成，先点停止");
            return;
        }
        Message user = new Message(Message.ROLE_USER, text == null ? "" : text);
        if (attachedImage != null) user.imagePath = attachedImage;
        if (!attachments.isEmpty()) {
            user.attachments.addAll(attachments);
            attachments.clear();
        }
        session.messages.add(user);
        if (attachedImage != null || !user.attachments.isEmpty()) {
            attachedImage = null;
            updateAttachChip();
        }
        if (session.title == null || session.title.length() == 0 || "新会话".equals(session.title)) {
            session.autoTitle();
            topBar.setTitle(session.title);
        }
        // 关键修复：发送成功后立即清空输入框，避免文字残留。
        input.clear();
        Message bot = new Message(Message.ROLE_ASSISTANT, "");
        bot.streaming = true;
        bot.finished = false;
        session.messages.add(bot);
        SessionStore.save(act, session);
        renderMessages(true);
        hideKeyboard();
        if (Experiment.agentEnabled(act)) startAgent(bot);
        else startStream(bot);
    }

    public void onStop() {
        if (agent != null) {
            agent.cancel();
            return;
        }
        if (request != null) request.cancel();
    }

    public void onPlus() {
        M3Sheet sheet = new M3Sheet(act).title("添加").subtitle("图片、任意附件，或调整这次请求");
        sheet.add(Icons.IMAGE, "从相册选择", new Runnable() {
            public void run() {
                act.pickImage(new MainActivity.ImagePickCallback() {
                    public void onImage(String path) {
                        attachedImage = path;
                        updateAttachChip();
                    }
                });
            }
        });
        sheet.add(Icons.CAMERA, "拍摄照片", new Runnable() {
            public void run() {
                act.takePhoto(new MainActivity.ImagePickCallback() {
                    public void onImage(String path) {
                        attachedImage = path;
                        updateAttachChip();
                    }
                });
            }
        });
        sheet.addDesc(Icons.DOC, "导入附件", "任意类型、不限制大小，复制到本机附件目录",
                new Runnable() {
                    public void run() {
                        act.pickFile(new MainActivity.FilePickCallback() {
                            public void onFile(String path) {
                                if (Attachments.isImage(path)) {
                                    attachedImage = path;
                                } else if (!attachments.contains(path)) {
                                    attachments.add(path);
                                }
                                updateAttachChip();
                                act.toast("已添加附件：" + new File(path).getName());
                            }
                        });
                    }
                });
        sheet.addDesc(Icons.FOLDER, "工作目录", AgentTools.rootLabel(act) + " · 让 Agent 读写文件",
                new Runnable() {
                    public void run() {
                        act.openWorkspace();
                    }
                });
        sheet.addDesc(Icons.TERMINAL, "终端", "Android 系统 shell · AI 可用 run_command",
                new Runnable() {
                    public void run() {
                        act.openTerminal();
                    }
                });
        final Provider p = Providers.current(act);
        if (p.isBuiltin()) {
            sheet.addDesc(Icons.WAIT, "思考模式 · 固定开启",
                    "内置模型是思考模型，不能关闭；换第三方模型才可调整", new Runnable() {
                        public void run() {
                            act.toast("内置模型固定思考，无法关闭");
                        }
                    });
        } else {
            sheet.addChecked(Icons.WAIT, "思考模式", Prefs.thinkingEnabled(act, p),
                    new Runnable() {
                        public void run() {
                            boolean next = !Prefs.thinkingEnabled(act, p);
                            Prefs.setThinkingEnabled(act, p, next);
                            act.toast(next ? "第三方模型已开启思考" : "第三方模型已关闭思考");
                        }
                    });
        }
        sheet.divider();
        sheet.add(Icons.BOOK, "查看知识库", new Runnable() {
            public void run() {
                act.push(new M3KnowledgePage(act));
            }
        });
        sheet.show(act.root().overlay);
    }

    private void updateAttachChip() {
        attachChip.removeAllViews();
        boolean has = attachedImage != null || !attachments.isEmpty();
        input.setHasAttachment(has);
        if (!has) {
            attachChip.setVisibility(GONE);
            return;
        }
        attachChip.setVisibility(VISIBLE);
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        attachChip.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        if (attachedImage != null) {
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            PhotoView pv = new PhotoView(act);
            pv.setPath(attachedImage);
            row.addView(pv, new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44)));
            Label name = new Label(act, "图片已就绪", 12f, M3.ON_PRIMARY_CONTAINER, false);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            nlp.leftMargin = Ui.dp(8);
            row.addView(name, nlp);
            IconButton close = new IconButton(act, Icons.CLOSE, 0, M3.ON_PRIMARY_CONTAINER, 26, 12);
            close.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    attachedImage = null;
                    updateAttachChip();
                }
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(26), Ui.dp(26));
            clp.leftMargin = Ui.dp(8);
            row.addView(close, clp);
            col.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        int shown = 0;
        for (int i = 0; i < attachments.size(); i++) {
            if (shown >= 3) break;
            final String path = attachments.get(i);
            final File f = new File(path);
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            if (shown > 0 || attachedImage != null) row.setPadding(0, Ui.dp(5), 0, 0);
            Label name = new Label(act, AgentTools.sizeText(f.length()) + " · " + f.getName(),
                    11.4f, M3.ON_PRIMARY_CONTAINER, false);
            row.addView(name, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            IconButton close = new IconButton(act, Icons.CLOSE, 0, M3.ON_PRIMARY_CONTAINER, 24, 11);
            close.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    attachments.remove(path);
                    updateAttachChip();
                }
            });
            row.addView(close, new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24)));
            col.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            shown++;
        }
        if (attachments.size() > 3) {
            Label more = new Label(act, "还有 " + (attachments.size() - 3) + " 个附件…", 10.8f,
                    M3.ON_PRIMARY_CONTAINER, false);
            more.setPadding(0, Ui.dp(4), 0, 0);
            col.addView(more, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    /** 3.0：经典主题也完整支持多轮 Agent。 */
    private void startAgent(final Message bot) {
        streaming = true;
        agentFinished = false;
        streamingMsg = bot;
        pendingContent = "";
        pendingReasoning = "";
        bot.agentLog = "";
        input.setStreaming(true);
        tickerRunning = false;
        topBar.setSubtitle("Agent · 第 1 轮");
        appendAgentLog(bot, "开始执行任务");

        agent = new AgentEngine(act, historyForRequest(), systemPromptForAgent(), new AgentEngine.Callback() {
            public void onRound(final int round) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        topBar.setSubtitle("Agent · 第 " + round + " 轮");
                    }
                });
            }

            public void onLog(final String line) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        appendAgentLog(bot, line);
                        if (streamingView != null) streamingView.updateContent();
                    }
                });
            }

            public void onFinal(final String text, final String reasoning,
                                final ArrayList<String> artifacts) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        if (agentFinished) return;
                        agentFinished = true;
                        bot.text = text == null ? "" : text;
                        bot.reasoning = reasoning == null ? "" : reasoning;
                        bot.artifacts.clear();
                        if (artifacts != null) {
                            bot.artifacts.addAll(artifacts);
                            // 下载工具默认写进 attachments：这里把它同步成消息附件，
                            // 气泡上会出现文件条，用户点一下就能打开。
                            String attachDir = Attachments.dir(act).getAbsolutePath();
                            for (int i = 0; i < artifacts.size(); i++) {
                                String p = artifacts.get(i);
                                if (p != null && p.startsWith(attachDir + File.separator)
                                        && !bot.attachments.contains(p)) {
                                    bot.attachments.add(p);
                                }
                            }
                        }
                        if (bot.artifacts.size() > 0) {
                            appendAgentLog(bot, "任务完成 · 产出 " + bot.artifacts.size() + " 个文件");
                        }
                        finishStream(false, false, null);
                        agent = null;
                        renderMessages(false);
                        maybeAutoReflect(bot);
                    }
                });
            }

            public void onError(final String message, final String detail) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        if (agentFinished) return;
                        agentFinished = true;
                        finishStream(false, true, message);
                        agent = null;
                        showError(message, detail, false);
                    }
                });
            }

            public void onStopped() {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        if (agentFinished) return;
                        agentFinished = true;
                        finishStream(true, false, null);
                        agent = null;
                    }
                });
            }
        });
        agent.start();
    }

    private String systemPromptForAgent() {
        String sys = session.systemPrompt == null ? "" : session.systemPrompt;
        String global = Prefs.getString(act, Prefs.KEY_SYSTEM_PROMPT, "");
        if (sys.length() == 0) sys = global;
        String profile = Profile.compose(act);
        if (profile.length() > 0) sys = sys + "\n\n" + profile;
        String skills = Skills.compose(act);
        if (skills.length() > 0) sys = sys + "\n\n" + skills;
        String flags = AgentFlags.compose(act);
        if (flags.length() > 0) sys = sys + "\n\n" + flags;
        String lang = Lang.instruction(act);
        if (lang.length() > 0) sys = sys + "\n" + lang;
        if (Prefs.getBool(act, Prefs.KEY_MEMORY, true)) {
            String kb = Knowledge.compose(act, 1200);
            if (kb.length() > 0) sys = sys + "\n\n" + kb;
        }
        String brief = Attachments.promptBrief(act, lastUserAttachments());
        if (brief.length() > 0) sys = sys + "\n\n" + brief;
        return sys;
    }

    private void appendAgentLog(Message bot, String line) {
        if (line == null || line.length() == 0) return;
        if (bot.agentLog.length() > 0) bot.agentLog = bot.agentLog + "\n";
        String[] lines = line.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) bot.agentLog = bot.agentLog + "\n";
            bot.agentLog = bot.agentLog + "· " + lines[i];
        }
    }

    private void maybeAutoReflect(final Message bot) {
        if (bot == null || !Prefs.reflectionEnabled(act)) return;
        if (bot.reflected || bot.error || bot.stopped) return;
        if (bot.text == null || bot.text.trim().length() < 8) return;
        if (!ReflectionEngine.looksWrong(bot.text)) return;
        reflect(bot, false);
    }

    private void reflect(final Message target, final boolean manual) {
        if (target == null || reflecting) {
            if (reflecting) act.toast("正在反思，稍等一下");
            return;
        }
        if (streaming) {
            act.toast("等这条回复结束后再反思");
            return;
        }
        reflecting = true;
        target.reflectionNote = manual ? "正在认真复查这条回复…" : "检测到可能的失误，正在反思…";
        if (!streaming) renderMessages(false);
        else if (streamingView != null) streamingView.updateContent();

        ReflectionEngine.run(act, session.messages, target, manual, new ReflectionEngine.Callback() {
            public void onResult(final boolean hasError, final String apology,
                                 final String fixed, final String reasoning) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        reflecting = false;
                        if (hasError && fixed != null && fixed.trim().length() > 0) {
                            target.text = fixed;
                            target.reflected = true;
                            String note = apology == null ? "" : apology;
                            if (reasoning != null && reasoning.length() > 0) {
                                note = note + (note.length() > 0 ? "\n\n" : "") + "判断依据：" + reasoning;
                            }
                            target.reflectionNote = note.length() == 0 ? "已重新修正这条回答。" : note;
                            act.toast("反思完成，已修正回答");
                        } else {
                            target.reflectionNote = apology == null ? "" : apology;
                        }
                        SessionStore.save(act, session);
                        if (!streaming) renderMessages(false);
                        else if (streamingView != null) streamingView.updateContent();
                    }
                });
            }

            public void onError(final String message) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        reflecting = false;
                        target.reflectionNote = "";
                        act.toast("反思失败：" + message);
                        if (!streaming) renderMessages(false);
                    }
                });
            }
        });
    }

    private void startStream(final Message bot) {
        streaming = true;
        streamingMsg = bot;
        pendingContent = "";
        pendingReasoning = "";
        input.setStreaming(true);
        dotPhase = 0;
        tickerRunning = true;
        topBar.setSubtitle("正在回复");
        Tween.post(subtitleTicker, 280);

        Provider provider = Providers.current(act);
        String sys = session.systemPrompt == null ? "" : session.systemPrompt;
        String global = Prefs.getString(act, Prefs.KEY_SYSTEM_PROMPT, "");
        if (sys.length() == 0) sys = global;
        String profile = Profile.compose(act);
        if (profile.length() > 0) sys = sys + "\n\n" + profile;
        String skills = Skills.compose(act);
        if (skills.length() > 0) sys = sys + "\n\n" + skills;
        String flags = AgentFlags.compose(act);
        if (flags.length() > 0) sys = sys + "\n\n" + flags;
        String lang = Lang.instruction(act);
        if (lang.length() > 0) sys = sys + "\n" + lang;
        boolean memory = Prefs.getBool(act, Prefs.KEY_MEMORY, true);
        if (memory) {
            String kb = Knowledge.compose(act, 1200);
            if (kb.length() > 0) sys = sys + "\n\n" + kb;
        }
        String brief = Attachments.promptBrief(act, lastUserAttachments());
        if (brief.length() > 0) sys = sys + "\n\n" + brief;

        request = ChatClient.send(act, provider, historyForRequest(), sys, memory, new ChatClient.Callback() {
            public void onStart() {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        // 保留顶栏的模型信息，避免打字点跳动
                    }
                });
            }

            public void onDelta(String content, String reasoning) {
                synchronized (lock) {
                    if (content != null) pendingContent = pendingContent + content;
                    if (reasoning != null) pendingReasoning = pendingReasoning + reasoning;
                    if (flushScheduled) return;
                    flushScheduled = true;
                }
                RequestQueue.get().postMainDelayed(flushTask, 40);
            }

            public void onDone(final boolean stopped) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        finishStream(stopped, false, null);
                        if (!stopped) maybeAutoReflect(bot);
                    }
                });
            }

            public void onError(final String message, final String detail, final boolean tls) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        finishStream(false, true, message);
                        showError(message, detail, tls);
                    }
                });
            }
        });
    }

    private ArrayList<Message> historyForRequest() {
        ArrayList<Message> list = new ArrayList<Message>();
        for (int i = 0; i < session.messages.size(); i++) {
            Message m = session.messages.get(i);
            if (m.streaming) continue;
            if (m.error) continue;
            if (m.text == null || (m.text.trim().length() == 0 && m.imagePath == null)) continue;
            list.add(m);
        }
        return list;
    }

    private void applyPending() {
        if (streamingMsg == null) return;
        String c;
        String r;
        synchronized (lock) {
            c = pendingContent;
            r = pendingReasoning;
            pendingContent = "";
            pendingReasoning = "";
        }
        if (c.length() == 0 && r.length() == 0) return;
        streamingMsg.text = streamingMsg.text + c;
        streamingMsg.reasoning = streamingMsg.reasoning + r;
        if (streamingView != null) streamingView.updateContent();
        if (Prefs.getBool(act, Prefs.KEY_AUTOSCROLL, true) && scroll.isNearBottom()) scrollToBottom();
    }

    private void finishStream(boolean stopped, boolean error, String message) {
        applyPending();
        streaming = false;
        tickerRunning = false;
        Tween.remove(subtitleTicker);
        input.setStreaming(false);
        if (streamingMsg != null) {
            streamingMsg.streaming = false;
            streamingMsg.finished = true;
            streamingMsg.error = error && streamingMsg.text.length() == 0;
            streamingMsg.stopped = stopped;
            if (streamingView != null) streamingView.updateContent();
        }
        if (request != null) {
            request.cancel();
            request = null;
        }
        SessionStore.save(act, session);
        topBar.setSubtitle(subtitle());
        updateQuickBar();
        if (streamingMsg != null && streamingMsg.text.length() == 0 && !error) {
            streamingMsg.text = "（没有收到内容，可能是模型返回为空）";
            if (streamingView != null) streamingView.updateContent();
        }
        scrollToBottom();
        streamingMsg = null;
        streamingView = null;
    }

    private void showError(String message, final String detail, boolean tls) {
        M3Sheet sheet = new M3Sheet(act).title("请求失败").subtitle(message);
        if (detail != null && detail.length() > 0) {
            sheet.addDesc(Icons.INFO, "查看技术细节",
                    detail.length() > 60 ? detail.substring(0, 60) + "…" : detail, new Runnable() {
                        public void run() {
                            M3Dialog.alert(act.root().overlay, "技术细节", detail);
                        }
                    });
        }
        if (tls) {
            sheet.addDesc(Icons.LOCK, "TLS 诊断",
                    "wolfSSL 兜底：" + (WolfSsl.isAvailable() ? "已加载" : "未加载"), new Runnable() {
                        public void run() {
                            M3Dialog.alert(act.root().overlay, "TLS 诊断",
                                    "平台 TLS：系统 JSSE\nwolfSSL："
                                            + (WolfSsl.isAvailable() ? WolfSsl.version() : "未加载")
                                            + "\n\n若为 HTTPS 端点且此处显示未加载，请先用 jni/build_native.sh 生成 .so。");
                        }
                    });
        }
        sheet.divider();
        sheet.add(Icons.REFRESH, "重试", new Runnable() {
            public void run() {
                regenerate();
            }
        });
        sheet.show(act.root().overlay);
    }

    private void regenerate() {
        if (streaming) return;
        int lastAssistant = -1;
        for (int i = session.messages.size() - 1; i >= 0; i--) {
            if (!session.messages.get(i).isUser()) {
                lastAssistant = i;
                break;
            }
        }
        if (lastAssistant >= 0) session.messages.remove(lastAssistant);
        if (session.messages.isEmpty()) return;
        Message bot = new Message(Message.ROLE_ASSISTANT, "");
        bot.streaming = true;
        bot.finished = false;
        session.messages.add(bot);
        SessionStore.save(act, session);
        renderMessages(true);
        if (Experiment.agentEnabled(act)) startAgent(bot);
        else startStream(bot);
    }

    private ArrayList<String> lastUserAttachments() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = session.messages.size() - 1; i >= 0; i--) {
            Message m = session.messages.get(i);
            if (m.isUser()) {
                for (int k = 0; k < m.attachments.size(); k++) list.add(m.attachments.get(k));
                if (m.imagePath != null) list.add(m.imagePath);
                break;
            }
        }
        return list;
    }

    // ------------------------------------------------------------ 消息操作

    /** 右上角三点进入管理菜单；消息本体不再绑定长按。 */
    public void onMenu(final Message msg) {
        onLongPress(msg);
    }

    public void onLongPress(final Message msg) {
        M3Sheet sheet = new M3Sheet(act).title(msg.isUser() ? "这条消息" : "ChatYUE 的回复");
        sheet.add(Icons.COPY, "复制文本", new Runnable() {
            public void run() {
                act.copyText(msg.text == null ? "" : msg.text);
            }
        });
        sheet.add(Icons.STAR, msg.starred ? "取消收藏" : "收藏这条", new Runnable() {
            public void run() {
                msg.starred = !msg.starred;
                SessionStore.save(act, session);
                act.toast(msg.starred ? "已收藏" : "已取消收藏");
            }
        });
        if (!msg.isUser()) {
            sheet.add(Icons.REFRESH, "重新生成", new Runnable() {
                public void run() {
                    regenerate();
                }
            });
            if (Prefs.reflectionEnabled(act) && !msg.reflected) {
                sheet.add(Icons.SPARKLE, "让它反思这条", new Runnable() {
                    public void run() {
                        reflect(msg, true);
                    }
                });
            }
            sheet.add(Icons.EDIT, "接着这条继续", new Runnable() {
                public void run() {
                    input.setText("接着上文继续：");
                    input.field().requestFocus();
                    showKeyboard();
                }
            });
        } else {
            sheet.add(Icons.EDIT, "编辑后重发", new Runnable() {
                public void run() {
                    input.setText(msg.text);
                    input.field().requestFocus();
                    showKeyboard();
                }
            });
        }
        sheet.divider();
        sheet.addDanger(Icons.TRASH, "删除这条", new Runnable() {
            public void run() {
                session.messages.remove(msg);
                SessionStore.save(act, session);
                renderMessages(false);
            }
        });
        sheet.show(act.root().overlay);
    }

    public void onDoubleTap(Message msg) {
        String text = msg == null || msg.text == null ? "" : msg.text;
        if (text.length() == 0) {
            act.toast("这条没有可复制的文本");
            return;
        }
        act.copyText(text);
        act.toast("已复制这条消息");
    }

    public void onCopyCode(String code) {
        act.copyText(code);
    }

    public void onImageTap(String path) {
        if (path == null) return;
        act.push(new M3ImagePreviewPage(act, path));
    }

    private String lastAssistantText() {
        for (int i = session.messages.size() - 1; i >= 0; i--) {
            Message m = session.messages.get(i);
            if (!m.isUser() && m.text != null && m.text.length() > 0) return m.text;
        }
        return "";
    }

    private void exportMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(session.title).append("\n\n");
        for (int i = 0; i < session.messages.size(); i++) {
            Message m = session.messages.get(i);
            sb.append(m.isUser() ? "**我**\n\n" : "**ChatYUE**\n\n");
            if (m.text != null) sb.append(m.text).append("\n\n");
        }
        act.copyText(sb.toString());
    }

    private void confirmClear() {
        M3Dialog.confirmDanger(act.root().overlay, "清空这段对话？", "消息会被删除，无法恢复。", "清空",
                new Runnable() {
                    public void run() {
                        session.messages.clear();
                        SessionStore.save(act, session);
                        renderMessages(false);
                        act.toast("已清空");
                    }
                });
    }

    private void openMoreMenu() {
        M3Sheet sheet = new M3Sheet(act).title(session.title).subtitle(subtitle());
        sheet.add(Icons.EDIT, "重命名会话", new Runnable() {
            public void run() {
                M3Dialog.input(act.root().overlay, "重命名", "会话标题", session.title,
                        new M3Dialog.InputCallback() {
                            public void onInput(String text) {
                                if (text.length() == 0) return;
                                session.title = text;
                                SessionStore.save(act, session);
                                topBar.setTitle(text);
                            }
                        });
            }
        });
        sheet.add(Icons.BOOK, "会话系统提示词", new Runnable() {
            public void run() {
                M3Dialog.input(act.root().overlay, "系统提示词", "为空则使用全局设置",
                        session.systemPrompt == null ? "" : session.systemPrompt,
                        new M3Dialog.InputCallback() {
                            public void onInput(String text) {
                                session.systemPrompt = text;
                                SessionStore.save(act, session);
                                act.toast("已保存，下一条生效");
                            }
                        });
            }
        });
        sheet.add(Icons.LIST, "全部会话", new Runnable() {
            public void run() {
                act.push(new M3SessionListPage(act));
            }
        });
        sheet.add(Icons.SHARE, "复制整段对话", new Runnable() {
            public void run() {
                exportMarkdown();
            }
        });
        sheet.add(Icons.SERVER, "配置服务器连接", new Runnable() {
            public void run() {
                act.openServerSettings();
            }
        });
        sheet.add(Icons.GEAR, "设置", new Runnable() {
            public void run() {
                act.openSettings();
            }
        });
        sheet.divider();
        sheet.addDanger(Icons.TRASH, "删除会话", new Runnable() {
            public void run() {
                M3Dialog.confirmDanger(act.root().overlay, "删除会话？",
                        session.title + " 将被永久删除。", "删除", new Runnable() {
                            public void run() {
                                SessionStore.delete(act, session.id);
                                act.pop();
                            }
                        });
            }
        });
        sheet.show(act.root().overlay);
    }

    private void showKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input.field(), 0);
        } catch (Throwable ignored) {
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(input.field().getWindowToken(), 0);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean onBack() {
        if (streaming) {
            onStop();
            act.toast("已停止生成");
            return true;
        }
        return false;
    }

    @Override
    public void onPageShown() {
        if (!streaming) topBar.setSubtitle(subtitle());
        scrollToBottom();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h > oldh) scrollToBottom();
    }

    private void updateQuickBar() {
        // 保持底栏高度恒定，避免发送瞬间输入条上下跳 44dp
        quickBar.setVisibility(View.VISIBLE);
    }
}
