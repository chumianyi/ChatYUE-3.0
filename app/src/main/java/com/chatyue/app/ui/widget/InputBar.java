package com.chatyue.app.ui.widget;

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
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 底部输入区：加号 + 多行输入 + 发送 / 停止键。
 *
 * 输入框本身只能用系统 EditText（键盘输入无法自绘），但外观完全接管：
 * 背景置空、内边距归零、颜色与字号全部指定，容器胶囊由 Canvas 绘制，
 * 因此看不到任何系统输入框的默认样式。
 */
public class InputBar extends ViewGroup implements TextWatcher {

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
    private boolean sendEnabled;
    private boolean hasAttachment;
    private final Paint bgPaint = Ui.fillPaint(0xFFEFEAEE);
    private final RectF rect = new RectF();
    private int padH;
    private int padV;

    public InputBar(Context c) {
        super(c);
        setWillNotDraw(false);
        padH = Ui.dp(9);
        padV = Ui.dp(8);

        edit = new EditText(c);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, Ui.dp(2), 0, Ui.dp(2));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        edit.setTextColor(Palette.INK);
        edit.setHintTextColor(Palette.INK_3);
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
        // 回车发送：设置里可关闭，关闭后回车就是换行
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

        plus = new IconButton(c, Icons.PLUS, 0xFFFFFFFF, Palette.INK, 34, 17);
        plus.setOnClickListener(new OnClickListener() {
            public void onClick(android.view.View v) {
                if (listener != null) listener.onPlus();
            }
        });
        addView(plus);

        send = new IconButton(c, Icons.SEND, 0xFFFFFFFF, Palette.INK_4, 38, 17);
        send.setOnClickListener(new OnClickListener() {
            public void onClick(android.view.View v) {
                if (listener == null) return;
                if (streaming) {
                    listener.onStop();
                    return;
                }
                doSend();
            }
        });
        addView(send);
        refreshSend();
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

    public void setStreaming(boolean s) {
        streaming = s;
        refreshSend();
    }

    public boolean isStreaming() {
        return streaming;
    }

    public EditText field() {
        return edit;
    }

    public void setHasAttachment(boolean has) {
        hasAttachment = has;
        refreshSend();
    }

    private void refreshSend() {
        String t = getText().trim();
        sendEnabled = t.length() > 0 || hasAttachment;
        if (streaming) {
            send.setIcon(Icons.STOP);
            send.setColors(Palette.DARK, 0xFFFFFFFF);
            send.setIconSize(13);
            send.setEnabledState(true);
        } else {
            send.setIcon(Icons.SEND);
            send.setIconSize(17);
            if (sendEnabled) {
                send.setColors(Palette.DARK, 0xFFFFFFFF);
                send.setEnabledState(true);
            } else {
                send.setColors(0xFFFFFFFF, Palette.INK_4);
                send.setEnabledState(true);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w = Ui.screenWidth() - Ui.dp(32);
        }
        plus.measure(MeasureSpec.makeMeasureSpec(Ui.dp(34), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(34), MeasureSpec.EXACTLY));
        send.measure(MeasureSpec.makeMeasureSpec(Ui.dp(38), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(38), MeasureSpec.EXACTLY));
        int editW = w - padH * 2 - Ui.dp(34) - Ui.dp(38) - Ui.dp(10) * 2;
        edit.measure(MeasureSpec.makeMeasureSpec(Math.max(Ui.dp(40), editW), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Ui.dp(110), MeasureSpec.AT_MOST));
        int h = Math.max(Ui.dp(50), edit.getMeasuredHeight() + padV * 2);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getWidth();
        int h = getHeight();
        int plusSize = Ui.dp(34);
        int plusLeft = padH;
        int plusTop = (h - plusSize) / 2;
        plus.layout(plusLeft, plusTop, plusLeft + plusSize, plusTop + plusSize);

        int sendSize = Ui.dp(38);
        int sendLeft = w - padH - sendSize;
        int sendTop = (h - sendSize) / 2;
        send.layout(sendLeft, sendTop, sendLeft + sendSize, sendTop + sendSize);

        int editLeft = plusLeft + plusSize + Ui.dp(10);
        int editRight = sendLeft - Ui.dp(10);
        int editH = edit.getMeasuredHeight();
        int editTop = (h - editH) / 2;
        edit.layout(editLeft, editTop, editRight, editTop + editH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        float r = Math.min(rect.height() / 2f, Ui.dpf(26));
        canvas.drawRoundRect(rect, r, r, bgPaint);
        super.onDraw(canvas);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
        refreshSend();
        requestLayout();
    }
}
