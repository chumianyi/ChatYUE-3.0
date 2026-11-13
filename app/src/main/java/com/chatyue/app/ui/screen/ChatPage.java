package com.chatyue.app.ui.screen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.ChatYueApp;
import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.AgentEngine;
import com.chatyue.app.data.AgentFlags;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.data.Attachments;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.data.Knowledge;
import com.chatyue.app.data.Lang;
import com.chatyue.app.data.Profile;
import com.chatyue.app.data.Skills;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Provider;
import com.chatyue.app.data.Providers;
import com.chatyue.app.data.ReflectionEngine;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.net.ChatClient;
import com.chatyue.app.net.Request;
import com.chatyue.app.net.RequestQueue;
import com.chatyue.app.net.WolfSsl;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.Avatar;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.Bubble;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.InputBar;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.TopBar;
import com.chatyue.app.util.TimeUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * 对话页：流式输出、中断、重生成、Markdown、代码复制、图片输入都在这里。
 */
public class ChatPage extends Page implements InputBar.Listener, Bubble.Listener {

    private final Session session;
    private final Object lock = new Object();

    private TopBar topBar;
    private BounceScrollView scroll;
    private LinearLayout transcript;
    private InputBar input;
    private LinearLayout quickBar;
    private RoundBox attachChip;

    private Bubble streamingBubble;
    private Message streamingMsg;
    private Request request;
    private boolean streaming;
    private String pendingContent = "";
    private String pendingReasoning = "";
    private boolean flushScheduled;
    private String attachedImage;
    private final ArrayList<String> attachments = new ArrayList<String>();
    private AgentEngine agent;
    private boolean agentRunning;
    private boolean reflecting;
    private boolean agentFinished;

    private final Runnable flushTask = new Runnable() {
        public void run() {
            synchronized (lock) {
                flushScheduled = false;
            }
            applyPending();
        }
    };

    public ChatPage(MainActivity a, Session s) {
        super(a);
        session = s != null ? s : SessionStore.create(a);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        topBar = new TopBar(a, session.title, subtitle(), Icons.MORE);
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
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(66)));

        scroll = new BounceScrollView(a);
        transcript = new LinearLayout(a);
        transcript.setOrientation(LinearLayout.VERTICAL);
        transcript.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(18));
        scroll.addView(transcript, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        quickBar = buildQuickBar();
        root.addView(quickBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(44)));

        LinearLayout inputWrap = new LinearLayout(a);
        inputWrap.setOrientation(LinearLayout.VERTICAL);
        inputWrap.setPadding(Ui.dp(14), Ui.dp(4), Ui.dp(14), Ui.dp(10));

        attachChip = new RoundBox(a);
        attachChip.setFill(Palette.PINK_TINT);
        attachChip.setRadius(14);
        attachChip.setStroke(Palette.PINK_LINE, 0.8f);
        attachChip.setPadding(Ui.dp(10), Ui.dp(6), Ui.dp(8), Ui.dp(6));
        attachChip.setVisibility(GONE);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        chipLp.bottomMargin = Ui.dp(8);
        inputWrap.addView(attachChip, chipLp);

        input = new InputBar(a);
        input.setListener(this);
        inputWrap.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(inputWrap, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        renderMessages();
        updateQuickBar();
    }

    private String subtitle() {
        int days = TimeUtil.daysSince(Prefs.firstLaunch(act));
        String model = Providers.current(act).name;
        return model + " · 陪伴 " + days + " 天";
    }

    private LinearLayout buildQuickBar() {
        LinearLayout bar = new LinearLayout(act);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);

        bar.addView(quickIcon(Icons.COPY, new Runnable() {
            public void run() {
                String text = lastAssistantText();
                if (text.length() == 0) act.toast("还没有可复制的内容");
                else act.copyText(text);
            }
        }));
        bar.addView(quickIcon(Icons.REFRESH, new Runnable() {
            public void run() {
                regenerate();
            }
        }));
        bar.addView(quickIcon(Icons.SHARE, new Runnable() {
            public void run() {
                exportMarkdown();
            }
        }));
        bar.addView(quickIcon(Icons.TRASH, new Runnable() {
            public void run() {
                confirmClear();
            }
        }));
        return bar;
    }

    private View quickIcon(int icon, final Runnable action) {
        IconButton b = new IconButton(act, icon, 0, Palette.INK_3, 32, 16);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(32), Ui.dp(32));
        lp.leftMargin = Ui.dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    // ------------------------------------------------------------ 消息渲染

    private void renderMessages() {
        transcript.removeAllViews();
        streamingBubble = null;
        if (session.messages.isEmpty()) {
            transcript.addView(buildWelcome(), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            for (int i = 0; i < session.messages.size(); i++) {
                Message m = session.messages.get(i);
                Bubble b = new Bubble(act, m);
                b.setListener(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = Ui.dp(i == 0 ? 4 : 10);
                transcript.addView(b, lp);
                if (m.streaming) {
                    streamingBubble = b;
                    streamingMsg = m;
                }
            }
        }
        scrollToBottom();
        updateQuickBar();
    }

    private View buildWelcome() {
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(0, Ui.dp(64), 0, 0);

        Avatar av = new Avatar(act);
        av.setBitmap(ChatYueApp.logo(act));
        av.setRing(Palette.PINK_SOFT, 4);
        col.addView(av, new LinearLayout.LayoutParams(Ui.dp(86), Ui.dp(86)));

        Label hi = new Label(act, "今天想聊点什么？", 16.4f, Palette.INK, false);
        hi.setAlign(Label.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(18);
        col.addView(hi, hlp);

        Label tip = new Label(act, "可以发文字、发图片，也可以让我写代码", 12.4f, Palette.INK_3, false);
        tip.setAlign(Label.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(6);
        col.addView(tip, tlp);

        final SoftButton mem = new SoftButton(act,
                "参考记忆：" + (Prefs.getBool(act, Prefs.KEY_MEMORY, true) ? "已开启" : "未开启"),
                11.6f, Palette.INK_2, 0xFFFFFFFF);
        mem.icon(Icons.DOT).pad(12, 7).radius(14).stroke(Palette.LINE, 0.8f);
        mem.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean next = !Prefs.getBool(act, Prefs.KEY_MEMORY, true);
                Prefs.setBool(act, Prefs.KEY_MEMORY, next);
                mem.setText("参考记忆：" + (next ? "已开启" : "未开启"));
            }
        });
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = Ui.dp(16);
        col.addView(mem, mlp);

        String[][] sugg = {
                {"帮我读一张图", "把图片里的内容讲给我听"},
                {"写段代码", "用 Java 写一个环形进度条"},
                {"理清思路", "我有个想法，帮我拆成三步"}
        };
        for (int i = 0; i < sugg.length; i++) {
            final String render = sugg[i][1];
            SoftButton chip = new SoftButton(act, sugg[i][0], 12.4f, Palette.INK_2, 0xFFFFFFFF);
            chip.pad(14, 8).radius(15).stroke(Palette.LINE, 0.8f);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    input.setText(render);
                    input.field().requestFocus();
                    showKeyboard();
                }
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.topMargin = Ui.dp(10);
            col.addView(chip, clp);
        }
        return col;
    }

    private void updateQuickBar() {
        quickBar.setVisibility(session.messages.isEmpty() ? View.GONE : View.VISIBLE);
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
        if (attachedImage != null) {
            attachedImage = null;
            updateAttachChip();
        } else if (!user.attachments.isEmpty()) {
            updateAttachChip();
        }
        if (session.title == null || session.title.length() == 0 || "新会话".equals(session.title)) {
            session.autoTitle();
            topBar.setTitle(session.title);
        }
        // 发送后立刻清空输入框：原来文本会一直留在 EditText 里，看起来像没发出去。
        input.clear();
        Message bot = new Message(Message.ROLE_ASSISTANT, "");
        bot.streaming = true;
        bot.finished = false;
        session.messages.add(bot);
        SessionStore.save(act, session);
        renderMessages();
        hideKeyboard();
        if (Experiment.agentEnabled(act)) startAgent(bot);
        else startStream(bot);
    }

    public void onStop() {
        if (agentRunning && agent != null) {
            agent.cancel();
            return;
        }
        if (request != null) request.cancel();
    }

    public void onPlus() {
        Sheet sheet = new Sheet(act).title("添加").subtitle("图片、任意附件，或调整这次请求");
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
            boolean on = Prefs.thinkingEnabled(act, p);
            sheet.addChecked(Icons.WAIT, "思考模式 · " + (on ? "开启" : "关闭"), on,
                    new Runnable() {
                        public void run() {
                            boolean next = !Prefs.thinkingEnabled(act, p);
                            Prefs.setThinkingEnabled(act, p, next);
                            act.toast(next ? "第三方模型已开启思考" : "第三方模型已关闭思考");
                        }
                    });
        }
        sheet.add(Icons.BOOK, "查看知识库", new Runnable() {
            public void run() {
                act.toast("知识库共有 " + Knowledge.all(act).size() + " 条片段");
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
            col.addView(imageRow(), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        int shown = 0;
        for (int i = 0; i < attachments.size(); i++) {
            if (shown >= 3) break;
            final String path = attachments.get(i);
            File f = new File(path);
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            if (shown > 0 || attachedImage != null) row.setPadding(0, Ui.dp(5), 0, 0);

            final View glyph = new View(act) {
                protected void onDraw(android.graphics.Canvas canvas) {
                    com.chatyue.app.core.Icons.draw(canvas,
                            Attachments.isImage(path) ? Icons.IMAGE : Icons.DOC,
                            getWidth() / 2f, getHeight() / 2f,
                            Math.min(getWidth(), getHeight()) * 0.72f, Palette.PINK_DEEP, 1.5f);
                }
            };
            row.addView(glyph, new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18)));
            Label name = new Label(act, AgentTools.sizeText(f.length()) + " · " + f.getName(),
                    11.4f, Palette.PINK_DEEP, false);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            nlp.leftMargin = Ui.dp(6);
            row.addView(name, nlp);
            IconButton close = new IconButton(act, Icons.CLOSE, 0, Palette.INK_3, 24, 11);
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
                    Palette.INK_3, false);
            more.setPadding(0, Ui.dp(4), 0, 0);
            col.addView(more, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private View imageRow() {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        com.chatyue.app.ui.widget.PhotoView pv = new com.chatyue.app.ui.widget.PhotoView(act);
        pv.setPath(attachedImage);
        row.addView(pv, new LinearLayout.LayoutParams(Ui.dp(40), Ui.dp(40)));
        Label name = new Label(act, "图片已就绪", 11.4f, Palette.PINK_DEEP, false);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.leftMargin = Ui.dp(8);
        row.addView(name, nlp);
        IconButton close = new IconButton(act, Icons.CLOSE, 0, Palette.INK_3, 24, 11);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attachedImage = null;
                updateAttachChip();
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24));
        clp.leftMargin = Ui.dp(8);
        row.addView(close, clp);
        return row;
    }

    /** 组织系统提示词：会话人设 + 画像 + 技能 + Agent 约束 + 语言 + 知识库 + 附件说明。 */
    private String systemPrompt() {
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
        return sys;
    }

    private ArrayList<String> lastUserAttachments() {
        for (int i = session.messages.size() - 1; i >= 0; i--) {
            Message m = session.messages.get(i);
            if (m.isUser()) {
                ArrayList<String> list = new ArrayList<String>();
                for (int k = 0; k < m.attachments.size(); k++) list.add(m.attachments.get(k));
                if (m.imagePath != null) list.add(m.imagePath);
                return list;
            }
        }
        return new ArrayList<String>();
    }

    private void startStream(final Message bot) {
        streaming = true;
        agentRunning = false;
        streamingMsg = bot;
        pendingContent = "";
        pendingReasoning = "";
        input.setStreaming(true);
        quickBar.setVisibility(View.GONE);
        topBar.setSubtitle("正在生成…");

        Provider provider = Providers.current(act);
        boolean memory = Prefs.getBool(act, Prefs.KEY_MEMORY, true);
        request = ChatClient.send(act, provider, historyForRequest(), systemPrompt(), memory, new ChatClient.Callback() {
            public void onStart() {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        topBar.setSubtitle("正在回复 · " + Providers.current(act).name);
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

    /** 3.0：多轮 Agent。每一轮都复用同一条助手消息，过程写进任务卡片。 */
    private void startAgent(final Message bot) {
        streaming = true;
        agentRunning = true;
        streamingMsg = bot;
        pendingContent = "";
        pendingReasoning = "";
        bot.agentLog = "";
        agentFinished = false;
        input.setStreaming(true);
        quickBar.setVisibility(View.GONE);
        topBar.setSubtitle("Agent · 第 1 轮");
        appendAgentLog(bot, "开始执行任务");

        agent = new AgentEngine(act, historyForRequest(), systemPrompt(), new AgentEngine.Callback() {
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
                        if (streamingBubble != null) streamingBubble.updateContent();
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
                        agentRunning = false;
                        agent = null;
                        renderMessages();
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
                        agentRunning = false;
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
                        agentRunning = false;
                        agent = null;
                    }
                });
            }
        });
        agent.start();
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

    // ------------------------------------------------------------ 反思

    private void maybeAutoReflect(final Message bot) {
        if (bot == null) return;
        if (!Prefs.reflectionEnabled(act)) return;
        if (bot.reflected || bot.error || bot.stopped) return;
        if (bot.text == null || bot.text.trim().length() < 8) return;
        if (!ReflectionEngine.looksWrong(bot.text)) return;
        reflect(bot, false);
    }

    /** 额外一次反思：确认错了就认错并替换成修复后的完整答案。 */
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
        if (streamingBubble == null || streamingBubble.message() != target) renderMessages();
        else streamingBubble.updateContent();

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
                        if (!streaming) renderMessages();
                        else if (streamingBubble != null) streamingBubble.updateContent();
                    }
                });
            }

            public void onError(final String message) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        reflecting = false;
                        target.reflectionNote = "";
                        act.toast("反思失败：" + message);
                        if (!streaming) renderMessages();
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
        if (streamingBubble != null) {
            streamingBubble.updateContent();
        }
        if (Prefs.getBool(act, Prefs.KEY_AUTOSCROLL, true) && scroll.isNearBottom()) scrollToBottom();
    }

    private void finishStream(boolean stopped, boolean error, String message) {
        applyPending();
        streaming = false;
        agentRunning = false;
        input.setStreaming(false);
        if (streamingMsg != null) {
            streamingMsg.streaming = false;
            streamingMsg.finished = true;
            streamingMsg.error = error && streamingMsg.text.length() == 0;
            streamingMsg.stopped = stopped;
            if (streamingBubble != null) streamingBubble.updateContent();
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
            if (streamingBubble != null) streamingBubble.updateContent();
        }
        scrollToBottom();
        streamingMsg = null;
        streamingBubble = null;
    }

    private void showError(String message, final String detail, boolean tls) {
        final Sheet sheet = new Sheet(act).title("请求失败").subtitle(message);
        if (detail != null && detail.length() > 0) {
            sheet.addDesc(Icons.INFO, "查看技术细节", detail.length() > 60 ? detail.substring(0, 60) + "…" : detail,
                    new Runnable() {
                        public void run() {
                            Dialog.alert(act.root().overlay, "技术细节", detail);
                        }
                    });
        }
        if (tls) {
            sheet.addDesc(Icons.LOCK, "TLS 诊断",
                    "wolfSSL 兜底：" + (WolfSsl.isAvailable() ? "已加载" : "未加载"),
                    new Runnable() {
                        public void run() {
                            Dialog.alert(act.root().overlay, "TLS 诊断",
                                    "平台 TLS：系统 JSSE\nwolfSSL："
                                            + (WolfSsl.isAvailable() ? WolfSsl.version() : "未加载")
                                            + "\n\n若为 HTTPS 端点且此处显示未加载，请先用 jni/build_native.sh 生成 .so。");
                        }
                    });
        }
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
        renderMessages();
        if (Experiment.agentEnabled(act)) startAgent(bot);
        else startStream(bot);
    }

    // ------------------------------------------------------------ 消息操作

    public void onLongPress(Message m) {
        final Message msg = m;
        Sheet sheet = new Sheet(act).title(m.isUser() ? "这条消息" : "ChatYUE 的回复");
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
        if (!m.isUser()) {
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
        sheet.addDanger(Icons.TRASH, "删除这条", new Runnable() {
            public void run() {
                session.messages.remove(msg);
                SessionStore.save(act, session);
                renderMessages();
            }
        });
        sheet.show(act.root().overlay);
    }

    public void onDoubleTap(Message m) {
        String text = m == null || m.text == null ? "" : m.text;
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

    public void onRetry(Message m) {
        regenerate();
    }

    public void onImageTap(String path) {
        if (path == null) return;
        act.push(new ImagePreviewPage(act, path));
    }

    public void onAttachmentTap(String path) {
        if (path == null) return;
        act.openFile(new File(path));
    }

    public void onPreviewHtml(String code) {
        act.previewHtmlCode(code);
    }

    public void onArtifactOpen(File f) {
        act.openFile(f);
    }

    public void onArtifactShare(File f) {
        act.shareArtifact(f);
    }

    public void onArtifactShareAll(ArrayList<String> paths) {
        act.shareArtifacts(paths);
    }

    public void onArtifactSave(File f) {
        act.saveArtifact(f);
    }

    public void onArtifactAll(ArrayList<String> paths) {
        act.push(new ArtifactListPage(act, paths));
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
        Dialog.confirmDanger(act.root().overlay, "清空这段对话？", "消息会被删除，无法恢复。", "清空",
                new Runnable() {
                    public void run() {
                        session.messages.clear();
                        SessionStore.save(act, session);
                        renderMessages();
                        act.toast("已清空");
                    }
                });
    }

    private void openMoreMenu() {
        Sheet sheet = new Sheet(act).title(session.title).subtitle(subtitle());
        sheet.add(Icons.EDIT, "重命名会话", new Runnable() {
            public void run() {
                Dialog.input(act.root().overlay, "重命名", "会话标题", session.title,
                        new Dialog.InputCallback() {
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
                Dialog.input(act.root().overlay, "系统提示词", "为空则使用全局设置",
                        session.systemPrompt == null ? "" : session.systemPrompt,
                        new Dialog.InputCallback() {
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
                act.push(new SessionListPage(act));
            }
        });
        sheet.add(Icons.SHARE, "复制整段对话", new Runnable() {
            public void run() {
                exportMarkdown();
            }
        });
        sheet.add(Icons.INFO, "会话信息与导出", new Runnable() {
            public void run() {
                act.push(new SessionDetailPage(act, session));
            }
        });
        sheet.add(Icons.GEAR, "设置", new Runnable() {
            public void run() {
                act.openSettings();
            }
        });
        sheet.addDanger(Icons.TRASH, "删除会话", new Runnable() {
            public void run() {
                Dialog.confirmDanger(act.root().overlay, "删除会话？", session.title + " 将被永久删除。",
                        "删除", new Runnable() {
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
            InputMethodManager imm = (InputMethodManager) act.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input.field(), 0);
        } catch (Throwable ignored) {
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
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
        topBar.setSubtitle(subtitle());
        scrollToBottom();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 键盘弹出 / 收起时保证视线停在最新一条
        if (h != oldh && !session.messages.isEmpty()) scrollToBottom();
    }

}
