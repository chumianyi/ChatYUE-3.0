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
import android.widget.EditText;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.IconButton;

/** M3 搜索框：浅色填充、胶囊圆角、左侧搜索图标，聚焦时描边变主色。 */
public class M3SearchField extends ViewGroup implements TextWatcher {

    public interface Listener {
        void onText(String text);
    }

    public final EditText edit;
    private final IconButton icon;
    private Listener listener;
    private float focus;
    private Tween focusTween;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public M3SearchField(final Context c, String hint) {
        super(c);
        setWillNotDraw(false);
        icon = new IconButton(c, Icons.SEARCH, 0, M3.ON_SURFACE_3, 34, 17);
        icon.setClickable(false);
        icon.setFocusable(false);
        addView(icon);

        edit = new EditText(c);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, 0, 0, 0);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.2f);
        edit.setTextColor(M3.ON_SURFACE);
        edit.setHintTextColor(M3.ON_SURFACE_3);
        edit.setHint(hint == null ? "" : hint);
        edit.setSingleLine(true);
        edit.setGravity(Gravity.CENTER_VERTICAL);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);
        edit.addTextChangedListener(this);
        edit.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(android.view.View v, boolean hasFocus) {
                animate(hasFocus ? 1f : 0f);
            }
        });
        addView(edit);

        setClickable(true);
        setOnClickListener(new OnClickListener() {
            public void onClick(android.view.View v) {
                edit.requestFocus();
                try {
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) c.getSystemService(
                                    android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(edit, 0);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    public M3SearchField listener(Listener l) {
        listener = l;
        return this;
    }

    public String getText() {
        return edit.getText().toString();
    }

    private void animate(float to) {
        if (focusTween != null) focusTween.cancel();
        focusTween = Tween.of(focus, to, 150, new Tween.Listener() {
            public void onUpdate(float v) {
                focus = v;
                invalidate();
            }

            public void onEnd() {
            }
        });
        focusTween.ease(Tween.STANDARD).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Ui.dp(50);
        icon.measure(MeasureSpec.makeMeasureSpec(Ui.dp(34), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Ui.dp(34), MeasureSpec.EXACTLY));
        edit.measure(MeasureSpec.makeMeasureSpec(Math.max(Ui.dp(40), w - Ui.dp(70)), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(Ui.dp(34), MeasureSpec.EXACTLY));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int h = getHeight();
        icon.layout(Ui.dp(10), (h - Ui.dp(34)) / 2, Ui.dp(44), (h - Ui.dp(34)) / 2 + Ui.dp(34));
        int left = Ui.dp(52);
        int right = getWidth() - Ui.dp(16);
        int eh = edit.getMeasuredHeight();
        int top = (h - eh) / 2;
        edit.layout(left, top, right, top + eh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        bg.setColor(Ui.mix(M3.SURFACE_LOW, M3.SURFACE, focus * 0.5f));
        canvas.drawRoundRect(rect, Ui.dpf(26), Ui.dpf(26), bg);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Ui.dpf(1));
        stroke.setColor(Ui.mix(M3.OUTLINE_VARIANT, M3.PRIMARY, focus));
        canvas.drawRoundRect(new RectF(Ui.dpf(0.5f), Ui.dpf(0.5f),
                getWidth() - Ui.dpf(0.5f), getHeight() - Ui.dpf(0.5f)),
                Ui.dpf(26), Ui.dpf(26), stroke);
        super.onDraw(canvas);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (listener != null) listener.onText(s == null ? "" : s.toString());
    }

    public void afterTextChanged(Editable s) {
    }
}
