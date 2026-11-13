package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.IconButton;

/**
 * M3 输入条：浅色圆角容器 + 左侧加号 + 右侧圆形发送键。
 * 聚焦时描边从 outline 渐变为 primary，发送键在可用/不可用之间切换颜色。
 */
public class M3InputBar extends ViewGroup implements TextWatcher {

    public interface Listener {
        void onSend(String text);

        void onStop();

        void onPlus();
    }

    public final EditText edit;
    public final IconButton plus;
    public final IconButton send;
    private Listener listener;
    private boolean streaming;
    private boolean hasAttachment;
    private float focus;
    private float sendProgress;
    private boolean enabledSend;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private com.chatyue.app.core.Tween focusTween;
    private com.chatyue.app.core.Tween sendTween;

    public M3InputBar(Context c) {
        super(c);
        setWillNotDraw(false);

        edit = new EditText(c);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, Ui.dp(2), 0, Ui.dp(2));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        edit.setTextColor(M3.ON_SURFACE);
        edit.setHintTextColor(M3.ON_SURFACE_3);
        edit.setHint("发消息…");
        edit.setSingleLine(false);
        edit.setMaxLines(5);
        edit.setMinLines(1);
        edit.setMinimumHeight(0);
        edit.setMinHeight(0);
        edit.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        edit.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setImeOptions(EditorInfo.IME_ACTION_NONE);
        edit.addTextChangedListener(this);
        edit.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(android.view.View v, boolean hasFocus) {
                animateFocus(hasFocus ? 1f : 0f);
            }
        });
        edit.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(android.view.View v, int keyCode, android.view.KeyEvent e) {
                if (keyCode == android.view.KeyEvent.KEYCODE_ENTER
                        && e.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    if (com.chatyue.app.data.Prefs.getBool(getContext(),
                            com.chatyue.app.data.Prefs.KEY_ENTER_SEND, true)) {
                        doSend();
                        return true;
                    }
                }
                return false;
            }
        });
        addView(edit);

        plus = new IconButton(c, Icons.PLUS, 0, M3.ON_SURFACE_2, 36, 18);
        plus.setOnClickListener(new OnClickListener() {
            public void onClick(android.view.View v) {
                if (listener != null) listener.onPlus();
            }
        });
        addView(plus);

        send = new IconButton(c, Icons.SEND, M3.SURFACE_CONTAINER, M3.ON_SURFACE_3, 40, 17);
        send.setOnClickListener(new OnClickListener() {
            public void onClick(android.view.View v) {
                if (listener == null) return;
                if (streaming) listener.onStop();
                else doSend();
            }
        });
        addView(send);
        refreshSend(false);
    }

    private void doSend() {
        if (listener == null) return;
        if (streaming) {
            listener.onStop();
            return;
        }
        String t = getText().trim();
        if (t.length() > 0 || hasAttachment) listener.onSend(t);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public String getText() {
        return edit.getText().toString();
    }

    public void setText(String s) {
        edit.setText(s == null ? "" : s);
        if (s != null) edit.setSelection(edit.getText().length());
    }

    public void clear() {
        edit.setText("");
    }

    public void setHint(String h) {
        edit.setHint(h);
    }

    public EditText field() {
        return edit;
    }

    public void setHasAttachment(boolean has) {
        hasAttachment = has;
        refreshSend(true);
    }

    public void setStreaming(boolean s) {
        streaming = s;
        refreshSend(true);
    }

    public boolean isStreaming() {
        return streaming;
    }

    private void refreshSend(boolean animate) {
        String t = getText().trim();
        boolean next = t.length() > 0 || hasAttachment;
        if (next != enabledSend || !animate) {
            enabledSend = next;
            if (animate) {
                if (sendTween != null) sendTween.cancel();
                sendTween = com.chatyue.app.core.Tween.of(sendProgress, next ? 1f : 0f, 140,
                        new com.chatyue.app.core.Tween.Listener() {
                            public void onUpdate(float v) {
                                sendProgress = v;
                                invalidate();
                            }

                            public void onEnd() {
                            }
                        });
                sendTween.ease(com.chatyue.app.core.Tween.STANDARD).start();
            } else {
                sendProgress = next ? 1f : 0f;
            }
        }
        if (streaming) {
            send.setIcon(Icons.STOP);
            send.setColors(M3.PRIMARY, M3.ON_PRIMARY);
            send.setIconSize(13);
        } else {
            send.setIcon(Icons.SEND);
            send.setIconSize(17);
            if (next) send.setColors(M3.PRIMARY, M3.ON_PRIMARY);
            else send.setColors(M3.SURFACE_CONTAINER, M3.ON_SURFACE_3);
        }
    }

    private void animateFocus(float to) {
        if (focusTween != null) focusTween.cancel();
        focusTween = com.chatyue.app.core.Tween.of(focus, to, 150, new com.chatyue.app.core.Tween.Listener() {
            public void onUpdate(float v) {
                focus = v;
                invalidate();
            }

            public void onEnd() {
            }
        });
        focusTween.ease(com.chatyue.app.core.Tween.STANDARD).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) w = Ui.screenWidth() - Ui.dp(28);
        plus.measure(MeasureSpec.makeMeasureSpec(Ui.dp(36), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(36), MeasureSpec.EXACTLY));
        send.measure(MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(40), MeasureSpec.EXACTLY));
        int editW = w - Ui.dp(16) * 2 - Ui.dp(36) - Ui.dp(40) - Ui.dp(10) * 2;
        edit.measure(MeasureSpec.makeMeasureSpec(Math.max(Ui.dp(40), editW), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Ui.dp(110), MeasureSpec.AT_MOST));
        int h = Math.max(Ui.dp(56), edit.getMeasuredHeight() + Ui.dp(18));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getWidth();
        int h = getHeight();
        int ps = Ui.dp(36);
        plus.layout(Ui.dp(10), (h - ps) / 2, Ui.dp(10) + ps, (h - ps) / 2 + ps);

        int ss = Ui.dp(40);
        int sl = w - Ui.dp(8) - ss;
        send.layout(sl, (h - ss) / 2, sl + ss, (h - ss) / 2 + ss);

        int editLeft = Ui.dp(10) + ps + Ui.dp(8);
        int editRight = sl - Ui.dp(8);
        int eh = edit.getMeasuredHeight();
        int et = (h - eh) / 2;
        edit.layout(editLeft, et, editRight, et + eh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        bgPaint.setColor(Ui.mix(M3.SURFACE_LOW, M3.SURFACE, focus * 0.6f));
        canvas.drawRoundRect(rect, Ui.dpf(28), Ui.dpf(28), bgPaint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Ui.dpf(1));
        strokePaint.setColor(Ui.mix(M3.OUTLINE_VARIANT, M3.PRIMARY, focus));
        canvas.drawRoundRect(new RectF(Ui.dpf(0.5f), Ui.dpf(0.5f),
                getWidth() - Ui.dpf(0.5f), getHeight() - Ui.dpf(0.5f)),
                Ui.dpf(28), Ui.dpf(28), strokePaint);
        // 发送键底部的极淡状态层，跟随可用性呼吸
        super.onDraw(canvas);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
        refreshSend(true);
        requestLayout();
    }
}
