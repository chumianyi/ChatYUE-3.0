package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Message;
import com.chatyue.app.md.MdInline;
import com.chatyue.app.ui.md.MarkdownView;
import com.chatyue.app.ui.md.MdText;

/**
 * 一条消息 = 头像（助手侧）+ 气泡 + 内容。
 *
 * 气泡背景、圆角、阴影、名字行全部自绘；内容区按角色不同装配：
 * 用户侧是纯文本（可含 Markdown 行内样式），助手侧是完整 Markdown + 思考卡片。
 */
public class Bubble extends ViewGroup {

    public interface Listener {
        void onLongPress(Message m);

        void onDoubleTap(Message m);

        void onCopyCode(String code);

        void onRetry(Message m);

        void onImageTap(String path);

        void onAttachmentTap(String path);

        void onPreviewHtml(String code);

        void onArtifactOpen(java.io.File f);

        void onArtifactShare(java.io.File f);

        void onArtifactShareAll(java.util.ArrayList<String> paths);

        void onArtifactSave(java.io.File f);

        void onArtifactAll(java.util.ArrayList<String> paths);
    }

    private final Message msg;
    private final Avatar avatar;
    private final LinearLayout content;
    private final MarkdownView body;
    private final MdText userText;
    private final ThinkingView thinking;
    private final ThinkingView taskView;
    private final RoundBox reflectionCard;
    private final PhotoView photo;
    private final Label footNote;
    private ArtifactCard artifactCard;

    private final int padL, padR, padT, padB;
    private final int avatarSize;
    private final int avatarGap;
    private final int nameH;
    private final RectF bubbleRect = new RectF();
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint namePaint = Ui.textPaint(Palette.INK_3, 10.4f, true);
    private final Path path = new Path();

    private Listener listener;
    private boolean autoCollapsed;
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
                if (listener != null) listener.onLongPress(msg);
            }
        }
    };

    public Bubble(Context c, Message m) {
        super(c);
        setWillNotDraw(false);
        this.msg = m;
        padL = Ui.dp(m.isUser() ? 14 : 13);
        padR = Ui.dp(m.isUser() ? 14 : 13);
        padT = Ui.dp(m.isUser() ? 10 : 11);
        padB = Ui.dp(m.isUser() ? 10 : 11);
        avatarSize = Ui.dp(28);
        avatarGap = Ui.dp(9);
        nameH = m.isUser() ? 0 : Ui.dp(16);

        avatar = new Avatar(c);
        avatar.setLetter("Y");
        avatar.setColors(Palette.PINK_SOFT, Palette.PINK_DEEP);
        if (!m.isUser()) addView(avatar);

        content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padL, padT, padR, padB);
        addView(content);

        taskView = (m.isUser() || (m.agentLog == null || m.agentLog.length() == 0)) ? null
                : new ThinkingView(c);
        if (taskView != null) {
            taskView.setTitle("任务过程");
            taskView.setReasoning(m.agentLog);
            taskView.setStreaming(m.streaming);
            content.addView(taskView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams tlp = (LinearLayout.LayoutParams) taskView.getLayoutParams();
            tlp.bottomMargin = Ui.dp(8);
            taskView.setLayoutParams(tlp);
        }

        // 思考卡片始终为助手侧创建，流式过程中 reasoning 才陆续到达
        thinking = m.isUser() ? null : new ThinkingView(c);
        if (thinking != null) {
            content.addView(thinking, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            thinking.setReasoning(m.reasoning);
            thinking.setStreaming(m.streaming);
            thinking.setVisibility(m.reasoning != null && m.reasoning.length() > 0 ? VISIBLE : GONE);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) thinking.getLayoutParams();
            lp.bottomMargin = Ui.dp(8);
            thinking.setLayoutParams(lp);
        }

        body = m.isUser() ? null : new MarkdownView(c, com.chatyue.app.data.Prefs.fontScale(c));
        if (body != null) {
            body.setCodeCopyListener(new com.chatyue.app.ui.md.CodeBox.CopyListener() {
                public void onCopy(String code) {
                    if (listener != null) listener.onCopyCode(code);
                }
            });
            body.setCodePreviewListener(new com.chatyue.app.ui.md.CodeBox.PreviewListener() {
                public void onPreview(String code) {
                    if (listener != null) listener.onPreviewHtml(code);
                }
            });
            body.setMarkdown(m.text);
            content.addView(body, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        userText = m.isUser() ? new MdText(c,
                MdInline.build(m.text, Palette.INK, Palette.PINK_DEEP),
                Ui.textPaint(Palette.INK, 14.8f * com.chatyue.app.data.Prefs.fontScale(c), false),
                0, 0, 4.2f) : null;
        if (userText != null) {
            content.addView(userText, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        photo = m.imagePath != null ? new PhotoView(c) : null;
        if (photo != null) {
            photo.setPath(m.imagePath);
            photo.setOnClickListener(new OnClickListener() {
                public void onClick(android.view.View v) {
                    if (listener != null) listener.onImageTap(msg.imagePath);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(7);
            content.addView(photo, 0, lp);
        }

        if (m.attachments != null && !m.attachments.isEmpty()) {
            content.addView(buildAttachments(m.attachments), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        if (!m.isUser() && m.artifacts != null && !m.artifacts.isEmpty()) {
            artifactCard = new ArtifactCard((MainActivity) c, m);
            artifactCard.setListener(new ArtifactCard.Listener() {
                public void onOpen(java.io.File f) {
                    if (listener != null) listener.onArtifactOpen(f);
                }

                public void onShare(java.io.File f) {
                    if (listener != null) listener.onArtifactShare(f);
                }

                public void onShareAll(java.util.ArrayList<String> paths) {
                    if (listener != null) listener.onArtifactShareAll(paths);
                }

                public void onSave(java.io.File f) {
                    if (listener != null) listener.onArtifactSave(f);
                }

                public void onSeeAll(java.util.ArrayList<String> paths) {
                    if (listener != null) listener.onArtifactAll(paths);
                }
            });
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            alp.topMargin = Ui.dp(8);
            content.addView(artifactCard, alp);
        }

        if (!m.isUser() && m.reflectionNote != null && m.reflectionNote.length() > 0) {
            reflectionCard = buildReflection(m.reflectionNote);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = Ui.dp(8);
            content.addView(reflectionCard, rlp);
        } else {
            reflectionCard = null;
        }

        footNote = new Label(c, "", 11f, Palette.INK_3, false);
        footNote.setVisibility(GONE);
        content.addView(footNote, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        refreshFoot();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public Message message() {
        return msg;
    }

    public void refreshFoot() {
        if (msg.error) {
            footNote.setText("发送失败 · 点按重试");
            footNote.setTextColor(Palette.RED);
            footNote.setVisibility(VISIBLE);
            footNote.setOnClickListener(new OnClickListener() {
                public void onClick(android.view.View v) {
                    if (listener != null) listener.onRetry(msg);
                }
            });
        } else if (msg.stopped) {
            footNote.setText("已停止生成");
            footNote.setTextColor(Palette.INK_3);
            footNote.setVisibility(VISIBLE);
        } else if (msg.streaming && msg.text.length() == 0 && (msg.reasoning == null || msg.reasoning.length() == 0)) {
            footNote.setText("正在生成…");
            footNote.setTextColor(Palette.INK_3);
            footNote.setVisibility(VISIBLE);
        } else {
            footNote.setVisibility(GONE);
        }
    }

    public void updateContent() {
        if (taskView != null) {
            taskView.setReasoning(msg.agentLog);
            taskView.setStreaming(msg.streaming);
        }
        if (thinking != null) {
            boolean has = msg.reasoning != null && msg.reasoning.length() > 0;
            if (!autoCollapsed && has && msg.text != null && msg.text.length() > 0) {
                thinking.collapse();
                autoCollapsed = true;
            }
            thinking.setReasoning(msg.reasoning);
            thinking.setStreaming(msg.streaming);
            thinking.setVisibility(has ? VISIBLE : GONE);
        }
        if (body != null) body.setMarkdown(msg.text);
        if (userText != null) {
            userText.setText(MdInline.build(msg.text, Palette.INK, Palette.PINK_DEEP));
        }
        refreshFoot();
        requestLayout();
        invalidate();
    }

    private View buildAttachments(java.util.ArrayList<String> paths) {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        int shown = 0;
        for (int i = 0; i < paths.size(); i++) {
            final String path = paths.get(i);
            if (path == null || path.length() == 0) continue;
            if (msg.imagePath != null && path.equals(msg.imagePath)) continue;
            java.io.File f = new java.io.File(path);
            if (!f.exists()) continue;
            if (shown > 0) {
                View gap = new View(getContext());
                col.addView(gap, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(4)));
            }
            col.addView(fileChip(f), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            shown++;
            if (shown >= 6) break;
        }
        return col;
    }

    private View fileChip(final java.io.File f) {
        RoundBox box = new RoundBox(getContext());
        box.setFill(Palette.BG_DEEP);
        box.setRadius(10);
        box.setPadding(Ui.dp(9), Ui.dp(6), Ui.dp(9), Ui.dp(6));
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        View glyph = new View(getContext()) {
            protected void onDraw(Canvas canvas) {
                int icon = com.chatyue.app.data.Attachments.isImage(f.getName())
                        ? Icons.IMAGE : Icons.DOC;
                Icons.draw(canvas, icon, getWidth() / 2f, getHeight() / 2f,
                        Math.min(getWidth(), getHeight()) * 0.7f, Palette.PINK_DEEP, 1.5f);
            }
        };
        row.addView(glyph, new LinearLayout.LayoutParams(Ui.dp(16), Ui.dp(16)));

        Label name = new Label(getContext(), f.getName(), 11.4f, Palette.INK_2, false);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.leftMargin = Ui.dp(6);
        row.addView(name, nlp);

        box.addView(row, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));
        box.setOnClickListener(new OnClickListener() {
            public void onClick(android.view.View v) {
                if (listener != null) listener.onAttachmentTap(f.getAbsolutePath());
            }
        });
        return box;
    }

    private RoundBox buildReflection(String note) {
        RoundBox box = new RoundBox(getContext());
        box.setFill(0xFFFFF7E8);
        box.setStroke(0xFFF0D9A8, 0.8f);
        box.setRadius(12);
        box.setPadding(Ui.dp(11), Ui.dp(9), Ui.dp(11), Ui.dp(9));
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        Label head = new Label(getContext(), "反思与修正", 11.2f, 0xFF9A6A12, true);
        col.addView(head, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        MdText bodyText = new MdText(getContext(), MdInline.build(note, Palette.INK_2, Palette.PINK_DEEP),
                Ui.textPaint(Palette.INK_2, 12.2f, false), 0, Ui.dp(4), 3.2f);
        col.addView(bodyText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(col, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.FILL_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentW = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            parentW = Ui.screenWidth() - Ui.dp(32);
        }
        int maxBubble = (int) (parentW * 0.865f);
        int reserved = msg.isUser() ? 0 : avatarSize + avatarGap;
        int availContent = Math.max(Ui.dp(80), maxBubble - reserved - padL - padR);
        content.measure(MeasureSpec.makeMeasureSpec(availContent, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int bubbleW = Math.min(maxBubble - reserved, content.getMeasuredWidth());
        int bubbleH = content.getMeasuredHeight();
        if (!msg.isUser()) {
            avatar.measure(MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(parentW, nameH + bubbleH + Ui.dp(8));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getWidth();
        int bubbleH = content.getMeasuredHeight();
        int bubbleW = Math.min((int) (w * 0.865f) - (msg.isUser() ? 0 : avatarSize + avatarGap),
                content.getMeasuredWidth());
        if (msg.isUser()) {
            int left = w - bubbleW;
            bubbleRect.set(left, 0, w, bubbleH);
            content.layout(left, 0, left + bubbleW, bubbleH);
        } else {
            avatar.layout(0, 0, avatarSize, avatarSize);
            int left = avatarSize + avatarGap;
            bubbleRect.set(left, nameH, left + bubbleW, nameH + bubbleH);
            content.layout(left, nameH, left + bubbleW, nameH + bubbleH);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 用户气泡改成真正的胶囊形：上下边平、左右两端椭圆，不再留方角。
        float tl = msg.isUser() ? 24 : 6;
        float tr = msg.isUser() ? 24 : 18;
        float br = msg.isUser() ? 24 : 18;
        float bl = msg.isUser() ? 24 : 18;
        path.reset();
        path.addRoundRect(bubbleRect, new float[]{
                Ui.dpf(tl), Ui.dpf(tl), Ui.dpf(tr), Ui.dpf(tr),
                Ui.dpf(br), Ui.dpf(br), Ui.dpf(bl), Ui.dpf(bl)}, Path.Direction.CW);
        if (!msg.isUser()) {
            // 助手气泡给一层很淡的投影，制造层次
            Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
            for (int i = 3; i >= 1; i--) {
                float grow = Ui.dpf(i * 1.6f);
                sp.setColor(Ui.alpha(Palette.SHADOW, 8));
                canvas.drawRoundRect(new RectF(bubbleRect.left - grow * 0.2f, bubbleRect.top + grow * 0.5f,
                        bubbleRect.right + grow * 0.2f, bubbleRect.bottom + grow), Ui.dpf(18), Ui.dpf(18), sp);
            }
        }
        bubblePaint.setColor(msg.isUser() ? Palette.USER_BUBBLE : Palette.BOT_BUBBLE);
        canvas.drawPath(path, bubblePaint);
        if (!msg.isUser()) {
            canvas.drawText("ChatYUE", avatarSize + avatarGap, Ui.baselineCentered(namePaint, nameH / 2f + Ui.dpf(1)), namePaint);
        }
        super.onDraw(canvas);
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
                // 2 秒长按必须足够“稳”：手指稍一滑动就取消，避免滑聊天记录时弹菜单。
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
                        if (listener != null) listener.onDoubleTap(msg);
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

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(longPress);
        super.onDetachedFromWindow();
    }
}
