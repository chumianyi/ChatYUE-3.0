package com.chatyue.app.ui.overlay;

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

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.md.MdText;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.MorphFrame;

/**
 * 自绘对话框：确认、提示、文本输入。
 * 没有 AlertDialog 的标题栏、分割线、系统按钮，全部按同一套圆角与动效绘制。
 */
public final class Dialog {

    public interface InputCallback {
        void onInput(String text);
    }

    private Dialog() {
    }

    public static void alert(OverlayHost host, String title, String message) {
        alert(host, title, message, "知道了", null);
    }

    public static void alert(final OverlayHost host, String title, String message, String okText,
                             final Runnable onOk) {
        Context c = host.getContext();
        LinearLayout col = column(c);
        if (title != null && title.length() > 0) {
            col.addView(new Label(c, title, 17.5f, Palette.INK, true), lpMatch());
        }
        if (message != null && message.length() > 0) {
            MdText body = new MdText(c, message, Ui.textPaint(Palette.INK_2, 13.6f, false), 0, 0, 4f);
            LinearLayout.LayoutParams p = lpMatch();
            p.topMargin = Ui.dp(9);
            col.addView(body, p);
        }
        SoftButton ok = new SoftButton(c, okText, 14.4f, 0xFFFFFFFF, Palette.DARK);
        ok.pad(16, 12).radius(14);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, onOk);
            }
        });
        LinearLayout.LayoutParams okLp = lpMatch();
        okLp.topMargin = Ui.dp(18);
        col.addView(ok, okLp);
        present(host, col, false);
    }

    public static void confirm(OverlayHost host, String title, String message, String okText,
                               Runnable onOk) {
        confirm(host, title, message, okText, "取消", Palette.DARK, onOk);
    }

    public static void confirmDanger(OverlayHost host, String title, String message, String okText,
                                     Runnable onOk) {
        confirm(host, title, message, okText, "取消", Palette.RED, onOk);
    }

    private static void confirm(final OverlayHost host, String title, String message, String okText,
                                String cancelText, final int okColor, final Runnable onOk) {
        Context c = host.getContext();
        LinearLayout col = column(c);
        col.addView(new Label(c, title, 17.5f, Palette.INK, true), lpMatch());
        if (message != null && message.length() > 0) {
            MdText body = new MdText(c, message, Ui.textPaint(Palette.INK_2, 13.6f, false), 0, 0, 4f);
            LinearLayout.LayoutParams p = lpMatch();
            p.topMargin = Ui.dp(9);
            col.addView(body, p);
        }
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = lpMatch();
        rlp.topMargin = Ui.dp(18);
        col.addView(row, rlp);

        SoftButton cancel = new SoftButton(c, cancelText, 14.2f, Palette.INK_2, Palette.BG_DEEP);
        cancel.pad(10, 12).radius(14);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.rightMargin = Ui.dp(5);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, null);
            }
        });
        row.addView(cancel, clp);

        SoftButton ok = new SoftButton(c, okText, 14.2f, 0xFFFFFFFF, okColor);
        ok.pad(10, 12).radius(14);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        olp.leftMargin = Ui.dp(5);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, onOk);
            }
        });
        row.addView(ok, olp);
        present(host, col, false);
    }

    public static void input(final OverlayHost host, String title, String hint, String initial,
                             final InputCallback cb) {
        final Context c = host.getContext();
        LinearLayout col = column(c);
        col.addView(new Label(c, title, 17.5f, Palette.INK, true), lpMatch());

        RoundBox box = new RoundBox(c);
        box.setFill(Palette.BG_DEEP);
        box.setRadius(14);
        box.setPadding(Ui.dp(12), Ui.dp(4), Ui.dp(12), Ui.dp(4));
        final EditText edit = new EditText(c);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.6f);
        edit.setTextColor(Palette.INK);
        edit.setHintTextColor(Palette.INK_4);
        edit.setHint(hint == null ? "" : hint);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);
        if (initial != null) {
            edit.setText(initial);
            edit.setSelection(initial.length());
        }
        box.addView(edit, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams blp = lpMatch();
        blp.topMargin = Ui.dp(12);
        col.addView(box, blp);

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = lpMatch();
        rlp.topMargin = Ui.dp(16);
        col.addView(row, rlp);

        SoftButton cancel = new SoftButton(c, "取消", 14.2f, Palette.INK_2, Palette.BG_DEEP);
        cancel.pad(10, 12).radius(14);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.rightMargin = Ui.dp(5);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(host, null);
            }
        });
        row.addView(cancel, clp);

        SoftButton ok = new SoftButton(c, "保存", 14.2f, 0xFFFFFFFF, Palette.DARK);
        ok.pad(10, 12).radius(14);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        olp.leftMargin = Ui.dp(5);
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
        row.addView(ok, olp);
        present(host, col, true);
        edit.postDelayed(new Runnable() {
            public void run() {
                edit.requestFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                c.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(edit, 0);
            }
        }, 260);
    }

    private static LinearLayout column(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(20), Ui.dp(20), Ui.dp(20), Ui.dp(18));
        return col;
    }

    private static LinearLayout.LayoutParams lpMatch() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static void present(final OverlayHost host, LinearLayout content, boolean centerOnly) {
        Context c = host.getContext();
        final RoundBox card = new RoundBox(c);
        card.setFill(Palette.CARD);
        card.setRadius(24);
        card.setShadow(Palette.SHADOW, 18);
        final int w = Math.min(Ui.dp(330), Ui.screenWidth() - Ui.dp(64));
        card.addView(content, new FrameLayout.LayoutParams(w, FrameLayout.LayoutParams.WRAP_CONTENT));

        final MorphFrame morph = new MorphFrame(c);
        morph.pivot(0.5f, 0.82f);
        morph.addView(card, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout wrap = new FrameLayout(c);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        wrap.addView(morph, lp);
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
        final int off = Ui.dp(22);
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
        if (after != null) Tween.post(after, 190);
    }
}
