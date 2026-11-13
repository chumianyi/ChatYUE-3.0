package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.core.Ui;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.data.Message;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.md.MdInline;
import com.chatyue.app.ui.md.MarkdownView;
import com.chatyue.app.ui.md.MdText;
import com.chatyue.app.ui.widget.ArtifactCard;
import com.chatyue.app.ui.widget.Avatar;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.PhotoView;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.ThinkingView;
import com.chatyue.app.ui.widget.TypingDots;
import com.chatyue.app.util.TimeUtil;

/**
 * M3 消息：用户是右对齐的 primary-container 气泡，助手是「头像 + 名字 + 无气泡正文」。
 * 助手侧刻意不用白卡片，避免整页都是盒子。
 */
public class M3MessageView extends LinearLayout {

    private final Message msg;
    private MarkdownView body;
    private ThinkingView thinking;
    private ThinkingView task;
    private ArtifactCard artifactCard;
    private TypingDots typing;
    private Label status;
    private MdText userText;
    private RoundBox userBubble;

    public interface GestureListener {
        void onLongPress(M3MessageView v);

        void onDoubleTap(M3MessageView v);
    }

    private GestureListener gestureListener;
    private float downX;
    private float downY;
    private boolean moved;
    private boolean longFired;
    private long lastTapAt;
    private float lastTapX;
    private float lastTapY;

    private final Runnable longPress = new Runnable() {
        public void run() {
            if (!moved && !longFired) {
                longFired = true;
                if (gestureListener != null) gestureListener.onLongPress(M3MessageView.this);
            }
        }
    };

    public void setGestureListener(GestureListener l) {
        gestureListener = l;
    }

    public Message getMessage() {
        return msg;
    }

    public M3MessageView(Context c, Message m) {
        super(c);
        msg = m;
        setOrientation(VERTICAL);
        if (m.isUser()) buildUser(c);
        else buildAssistant(c);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & 0xff) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                moved = false;
                longFired = false;
                removeCallbacks(longPress);
                postDelayed(longPress, 2000);
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(ev.getX() - downX) > Ui.dpf(5) || Math.abs(ev.getY() - downY) > Ui.dpf(5)) {
                    moved = true;
                    removeCallbacks(longPress);
                }
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                if (!moved && !longFired) {
                    long now = SystemClock.uptimeMillis();
                    if (lastTapAt > 0 && now - lastTapAt <= 320
                            && Math.abs(ev.getX() - lastTapX) <= Ui.dpf(7)
                            && Math.abs(ev.getY() - lastTapY) <= Ui.dpf(7)) {
                        lastTapAt = 0;
                        if (gestureListener != null) gestureListener.onDoubleTap(this);
                    } else {
                        lastTapAt = now;
                        lastTapX = ev.getX();
                        lastTapY = ev.getY();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                break;
        }
        return false;
    }

    private void buildUser(Context c) {
        LinearLayout wrap = new LinearLayout(c);
        wrap.setOrientation(VERTICAL);
        wrap.setGravity(Gravity.RIGHT);
        addView(wrap, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (msg.imagePath != null) {
            PhotoView pv = new PhotoView(c);
            pv.setPath(msg.imagePath);
            pv.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (getContext() instanceof com.chatyue.app.MainActivity) {
                        ((com.chatyue.app.MainActivity) getContext()).push(
                                new com.chatyue.app.ui.m3.M3ImagePreviewPage(
                                        (com.chatyue.app.MainActivity) getContext(), msg.imagePath));
                    }
                }
            });
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(UiPhoto.width(c), UiPhoto.height(c, msg.imagePath));
            plp.gravity = Gravity.RIGHT;
            plp.bottomMargin = UiPhoto.gap(c);
            wrap.addView(pv, plp);
        }

        for (int i = 0; i < msg.attachments.size(); i++) {
            final java.io.File f = new java.io.File(msg.attachments.get(i));
            if (!f.exists()) continue;
            Label chip = new Label(c, AgentTools.sizeText(f.length()) + " · " + f.getName(),
                    10.8f, M3.ON_PRIMARY_CONTAINER, false);
            chip.setPadding(Ui.dp(8), Ui.dp(4), Ui.dp(8), Ui.dp(4));
            chip.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (getContext() instanceof com.chatyue.app.MainActivity) {
                        ((com.chatyue.app.MainActivity) getContext()).openFile(f);
                    }
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.RIGHT;
            lp.bottomMargin = UiPhoto.gap(c);
            wrap.addView(chip, lp);
        }

        userBubble = new RoundBox(c);
        userBubble.setFill(M3.PRIMARY_CONTAINER);
        userBubble.corners(24, 24, 24, 24);
        userBubble.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
        userText = new MdText(c, MdInline.build(msg.text, M3.ON_PRIMARY_CONTAINER, M3.PRIMARY_DEEP),
                Ui.textPaint(M3.ON_PRIMARY_CONTAINER, 14.6f, false), 0, 0, 5f);
        userBubble.addView(userText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.RIGHT;
        wrap.addView(userBubble, blp);

        Label time = new Label(c, TimeUtil.friendly(msg.time), 9.6f, M3.ON_SURFACE_3, false);
        time.setAlign(Label.RIGHT);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.gravity = Gravity.RIGHT;
        tlp.topMargin = UiPhoto.gap(c);
        tlp.rightMargin = Ui.dp(14);
        wrap.addView(time, tlp);
    }

    private void buildAssistant(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(HORIZONTAL);
        addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Avatar av = new Avatar(c);
        av.setLetter("C");
        av.setColors(M3.PRIMARY_CONTAINER, M3.PRIMARY_DEEP);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(UiPhoto.avatar(c), UiPhoto.avatar(c));
        alp.rightMargin = UiPhoto.gap(c);
        row.addView(av, alp);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(VERTICAL);
        row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        Label name = new Label(c, "ChatYUE", 12.2f, M3.PRIMARY_DEEP, true);
        head.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Label time = new Label(c, "  " + TimeUtil.friendly(msg.time), 9.6f, M3.ON_SURFACE_3, false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = UiPhoto.gap(c);
        head.addView(time, tlp);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, UiPhoto.headHeight(c));
        col.addView(head, hlp);

        thinking = new ThinkingView(c);
        thinking.setReasoning(msg.reasoning);
        thinking.setStreaming(msg.streaming);
        thinking.setVisibility(msg.reasoning != null && msg.reasoning.length() > 0 ? VISIBLE : GONE);
        LinearLayout.LayoutParams thlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        thlp.bottomMargin = UiPhoto.gap(c);
        col.addView(thinking, thlp);

        task = new ThinkingView(c);
        task.setTitle("任务过程");
        task.setReasoning(msg.agentLog);
        task.setStreaming(msg.streaming);
        task.setVisibility(msg.agentLog != null && msg.agentLog.length() > 0 ? VISIBLE : GONE);
        LinearLayout.LayoutParams taskLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        taskLp.bottomMargin = UiPhoto.gap(c);
        col.addView(task, taskLp);

        if (msg.imagePath != null) {
            PhotoView pv = new PhotoView(c);
            pv.setPath(msg.imagePath);
            pv.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (getContext() instanceof com.chatyue.app.MainActivity) {
                        ((com.chatyue.app.MainActivity) getContext()).push(
                                new com.chatyue.app.ui.m3.M3ImagePreviewPage(
                                        (com.chatyue.app.MainActivity) getContext(), msg.imagePath));
                    }
                }
            });
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    UiPhoto.width(c), UiPhoto.height(c, msg.imagePath));
            plp.bottomMargin = UiPhoto.gap(c);
            col.addView(pv, plp);
        }

        body = new MarkdownView(c, Prefs.fontScale(c));
        body.setMarkdown(msg.text);
        body.setCodeCopyListener(new com.chatyue.app.ui.md.CodeBox.CopyListener() {
            public void onCopy(String code) {
                if (getContext() instanceof com.chatyue.app.MainActivity) {
                    ((com.chatyue.app.MainActivity) getContext()).copyText(code);
                }
            }
        });
        body.setCodePreviewListener(new com.chatyue.app.ui.md.CodeBox.PreviewListener() {
            public void onPreview(String code) {
                if (getContext() instanceof com.chatyue.app.MainActivity) {
                    ((com.chatyue.app.MainActivity) getContext()).previewHtmlCode(code);
                }
            }
        });
        col.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (!msg.attachments.isEmpty()) {
            LinearLayout files = new LinearLayout(c);
            files.setOrientation(LinearLayout.VERTICAL);
            for (int i = 0; i < msg.attachments.size(); i++) {
                final java.io.File f = new java.io.File(msg.attachments.get(i));
                if (!f.exists()) continue;
                Label chip = new Label(c, AgentTools.sizeText(f.length()) + " · " + f.getName(),
                        10.8f, M3.ON_SURFACE_2, false);
                chip.setPadding(Ui.dp(8), Ui.dp(4), Ui.dp(8), Ui.dp(4));
                chip.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (getContext() instanceof com.chatyue.app.MainActivity) {
                            ((com.chatyue.app.MainActivity) getContext()).openFile(f);
                        }
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = UiPhoto.gap(c);
                files.addView(chip, lp);
            }
            if (files.getChildCount() > 0) {
                col.addView(files, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }

        if (!msg.artifacts.isEmpty()) {
            final com.chatyue.app.MainActivity act =
                    getContext() instanceof com.chatyue.app.MainActivity
                            ? (com.chatyue.app.MainActivity) getContext() : null;
            artifactCard = new ArtifactCard((com.chatyue.app.MainActivity) getContext(), msg);
            artifactCard.setListener(new ArtifactCard.Listener() {
                public void onOpen(java.io.File f) {
                    if (act != null) act.openFile(f);
                }

                public void onShare(java.io.File f) {
                    if (act != null) act.shareArtifact(f);
                }

                public void onShareAll(java.util.ArrayList<String> paths) {
                    if (act != null) act.shareArtifacts(paths);
                }

                public void onSave(java.io.File f) {
                    if (act != null) act.saveArtifact(f);
                }

                public void onSeeAll(java.util.ArrayList<String> paths) {
                    if (act != null) act.push(new com.chatyue.app.ui.screen.ArtifactListPage(act, paths));
                }
            });
            LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            al.topMargin = UiPhoto.gap(c);
            col.addView(artifactCard, al);
        }

        if (msg.reflectionNote != null && msg.reflectionNote.length() > 0) {
            RoundBox note = new RoundBox(c);
            note.setFill(0xFFFFF7E8);
            note.setStroke(0xFFF0D9A8, 0.8f);
            note.setRadius(12);
            note.setPadding(Ui.dp(11), Ui.dp(8), Ui.dp(11), Ui.dp(8));
            Label ref = new Label(c, "反思与修正", 10.8f, 0xFF9A6A12, true);
            note.addView(ref, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            MdText rt = new MdText(c, MdInline.build(msg.reflectionNote, M3.ON_SURFACE_2, M3.PRIMARY),
                    Ui.textPaint(M3.ON_SURFACE_2, 11.6f, false), 0, 0, 2.6f);
            note.addView(rt, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = UiPhoto.gap(c);
            col.addView(note, rlp);
        }

        typing = new TypingDots(c);
        typing.setColor(M3.PRIMARY);
        LinearLayout.LayoutParams typ = new LinearLayout.LayoutParams(UiPhoto.typingWidth(c), UiPhoto.typingHeight(c));
        typ.topMargin = UiPhoto.gap(c);
        col.addView(typing, typ);
        if (msg.streaming && (msg.text == null || msg.text.length() == 0)) {
            typing.setVisibility(VISIBLE);
            typing.start();
        } else {
            typing.setVisibility(GONE);
        }

        status = new Label(c, "", 10.6f, M3.ON_SURFACE_3, false);
        status.setVisibility(GONE);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = UiPhoto.gap(c);
        col.addView(status, slp);
        refreshStatus();
    }

    private void refreshStatus() {
        if (status == null) return;
        if (msg.error) {
            status.setText("发送失败 · 点按重试");
            status.setTextColor(M3.ERROR);
            status.setVisibility(VISIBLE);
        } else if (msg.stopped) {
            status.setText("已停止生成");
            status.setVisibility(VISIBLE);
        } else {
            status.setVisibility(GONE);
        }
    }

    public void updateContent() {
        if (body != null) body.setMarkdown(msg.text);
        if (thinking != null) {
            thinking.setReasoning(msg.reasoning);
            thinking.setStreaming(msg.streaming);
            thinking.setVisibility(msg.reasoning != null && msg.reasoning.length() > 0 ? VISIBLE : GONE);
        }
        if (task != null) {
            task.setReasoning(msg.agentLog);
            task.setStreaming(msg.streaming);
            task.setVisibility(msg.agentLog != null && msg.agentLog.length() > 0 ? VISIBLE : GONE);
        }
        if (typing != null) {
            if (msg.streaming && (msg.text == null || msg.text.length() == 0)) {
                typing.setVisibility(VISIBLE);
                typing.start();
            } else {
                typing.stop();
                typing.setVisibility(GONE);
            }
        }
        refreshStatus();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(longPress);
        super.onDetachedFromWindow();
    }

    /** 小工具：统一尺寸，避免每个子控件写一遍 dp。 */
    private static final class UiPhoto {
        static int pad(Context c) {
            return com.chatyue.app.core.Ui.dp(14);
        }

        static int padV(Context c) {
            return com.chatyue.app.core.Ui.dp(10);
        }

        static int gap(Context c) {
            return com.chatyue.app.core.Ui.dp(7);
        }

        static int avatar(Context c) {
            return com.chatyue.app.core.Ui.dp(30);
        }

        static int headHeight(Context c) {
            return com.chatyue.app.core.Ui.dp(24);
        }

        static int width(Context c) {
            return Math.min(com.chatyue.app.core.Ui.dp(230), com.chatyue.app.core.Ui.screenWidth() - com.chatyue.app.core.Ui.dp(110));
        }

        static int height(Context c, String path) {
            return width(c) * 2 / 3;
        }

        static int typingWidth(Context c) {
            return com.chatyue.app.core.Ui.dp(44);
        }

        static int typingHeight(Context c) {
            return com.chatyue.app.core.Ui.dp(22);
        }
    }
}
