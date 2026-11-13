package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.md.MdText;
import com.chatyue.app.ui.overlay.OverlayHost;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MorphFrame;
import com.chatyue.app.ui.widget.RoundBox;

/** M3 对话框：28dp 圆角、标题 22sp、右下角文字/填充按钮，98ms 放大浮现。 */
public final class M3Dialog {

    public interface InputCallback {
        void onInput(String text);
    }

    private M3Dialog() {
    }

    public static void alert(OverlayHost host, String title, String message) {
        alert(host, title, message, "知道了", null);
    }

    public static void alert(final OverlayHost host, String title, String message,
                             String okText, final Runnable onOk) {
        Context c = host.getContext();
        LinearLayout col = column(c);
        col.addView(new Label(c, title, 22f, M3.ON_SURFACE, true), lpMatch());
        if (message != null && message.length() > 0) {
            MdText body = new MdText(c, message, Ui.textPaint(M3.ON_SURFACE_2, 14f, false), 0, 0, 4f);
            LinearLayout.LayoutParams p = lpMatch();
            p.topMargin = Ui.dp(10);
            col.addView(body, p);
        }
        LinearLayout row = actionRow(c);
        M3Button ok = new M3Button(c, okText, M3Button.TEXT);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, onOk);
            }
        });
        row.addView(ok, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(44)));
        col.addView(row, lpMatch());
        present(host, col);
    }

    public static void confirm(final OverlayHost host, String title, String message,
                               String okText, final Runnable onOk) {
        confirm(host, title, message, okText, "取消", M3Button.TONAL, onOk);
    }

    public static void confirmDanger(final OverlayHost host, String title, String message,
                                     String okText, final Runnable onOk) {
        confirm(host, title, message, okText, "取消", M3Button.FILLED, onOk);
    }

    private static void confirm(final OverlayHost host, String title, String message,
                                String okText, String cancelText, int okStyle, final Runnable onOk) {
        Context c = host.getContext();
        LinearLayout col = column(c);
        col.addView(new Label(c, title, 22f, M3.ON_SURFACE, true), lpMatch());
        if (message != null && message.length() > 0) {
            MdText body = new MdText(c, message, Ui.textPaint(M3.ON_SURFACE_2, 14f, false), 0, 0, 4f);
            LinearLayout.LayoutParams p = lpMatch();
            p.topMargin = Ui.dp(10);
            col.addView(body, p);
        }
        LinearLayout row = actionRow(c);
        M3Button cancel = new M3Button(c, cancelText, M3Button.TEXT);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, null);
            }
        });
        row.addView(cancel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(44)));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(Ui.dp(6), Ui.dp(1));
        row.addView(new View(c), gap);
        M3Button ok = new M3Button(c, okText, okStyle);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, onOk);
            }
        });
        row.addView(ok, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(44)));
        col.addView(row, lpMatch());
        present(host, col);
    }

    public static void input(final OverlayHost host, String title, String hint, String initial,
                             final InputCallback cb) {
        final Context c = host.getContext();
        LinearLayout col = column(c);
        col.addView(new Label(c, title, 22f, M3.ON_SURFACE, true), lpMatch());

        RoundBox box = new RoundBox(c);
        box.setFill(M3.SURFACE_CONTAINER);
        box.setRadius(20);
        box.setPadding(Ui.dp(14), Ui.dp(5), Ui.dp(14), Ui.dp(5));
        final EditText edit = new EditText(c);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.6f);
        edit.setTextColor(M3.ON_SURFACE);
        edit.setHintTextColor(M3.ON_SURFACE_3);
        edit.setHint(hint == null ? "" : hint);
        edit.setSingleLine(false);
        edit.setMaxLines(4);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (initial != null) {
            edit.setText(initial);
            edit.setSelection(initial.length());
        }
        box.addView(edit, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams blp = lpMatch();
        blp.topMargin = Ui.dp(14);
        col.addView(box, blp);

        LinearLayout row = actionRow(c);
        M3Button cancel = new M3Button(c, "取消", M3Button.TEXT);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, null);
            }
        });
        row.addView(cancel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(44)));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(Ui.dp(6), Ui.dp(1));
        row.addView(new View(c), gap);
        M3Button ok = new M3Button(c, "保存", M3Button.FILLED);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String text = edit.getText().toString().trim();
                hide(host, new Runnable() {
                    public void run() {
                        if (cb != null) cb.onInput(text);
                    }
                });
            }
        });
        row.addView(ok, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(44)));
        col.addView(row, lpMatch());
        present(host, col);

        edit.postDelayed(new Runnable() {
            public void run() {
                edit.requestFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                c.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(edit, 0);
            }
        }, 240);
    }

    private static LinearLayout column(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(24), Ui.dp(24), Ui.dp(24), Ui.dp(18));
        return col;
    }

    private static LinearLayout actionRow(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams p = lpMatch();
        p.topMargin = Ui.dp(18);
        row.setLayoutParams(p);
        return row;
    }

    private static LinearLayout.LayoutParams lpMatch() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static void present(final OverlayHost host, LinearLayout content) {
        Context c = host.getContext();
        final MorphFrame morph = new MorphFrame(c);
        morph.pivot(0.5f, 0.86f);

        RoundBox card = new RoundBox(c);
        card.setFill(M3.SURFACE);
        card.setRadius(28);
        card.setShadow(0x332A1B22, 22);
        final int w = Math.min(Ui.dp(340), Ui.screenWidth() - Ui.dp(56));
        card.addView(content, new FrameLayout.LayoutParams(w, FrameLayout.LayoutParams.WRAP_CONTENT));
        morph.addView(card, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout wrap = new FrameLayout(c);
        wrap.addView(morph, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        host.show(new Runnable() {
            public void run() {
                hide(host, null);
            }
        });
        host.setCloseAction(new Runnable() {
            public void run() {
                hide(host, null);
            }
        });
        final int off = Ui.dp(20);
        morph.transform(0f, 0.86f, 0f, off);
        host.layer().addView(wrap, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        morph.post(new Runnable() {
            public void run() {
                Tween.of(0, 1, 98, new Tween.Listener() {
                    public void onUpdate(float v) {
                        morph.transform(v, 0.86f + 0.14f * v, 0f, (int) (off * (1f - v)));
                    }

                    public void onEnd() {
                        morph.transform(1f, 1f, 0f, 0f);
                    }
                }).ease(Tween.DECEL).start();
            }
        });
    }

    private static void hide(final OverlayHost host, final Runnable after) {
        if (!host.isOpen()) {
            if (after != null) after.run();
            return;
        }
        host.hide();
        if (after != null) Tween.post(after, 130);
    }
}
