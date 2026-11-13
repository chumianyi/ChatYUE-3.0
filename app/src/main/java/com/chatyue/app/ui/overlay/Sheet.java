package com.chatyue.app.ui.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.MorphFrame;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.SoftButton;

import java.util.ArrayList;

/**
 * 自绘底部菜单。从底部滑入、点遮罩消失、可被返回键关闭，
 * 外观与系统 PopupMenu / Dialog 完全不同。
 */
public class Sheet {

    public static class Item {
        public int icon;
        public String title = "";
        public String desc = "";
        public String letter;
        public boolean danger;
        public boolean checked;
        public Runnable action;
    }

    private final Context ctx;
    private final ArrayList<Item> items = new ArrayList<Item>();
    private String title;
    private String subtitle;
    private boolean dismissing;

    public Sheet(Context c) {
        ctx = c;
    }

    public Sheet title(String t) {
        title = t;
        return this;
    }

    public Sheet subtitle(String s) {
        subtitle = s;
        return this;
    }

    public Sheet add(int icon, String t, Runnable r) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.action = r;
        items.add(it);
        return this;
    }

    public Sheet addDesc(int icon, String t, String d, Runnable r) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.desc = d;
        it.action = r;
        items.add(it);
        return this;
    }

    public Sheet addChecked(int icon, String t, boolean checked, Runnable r) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.checked = checked;
        it.action = r;
        items.add(it);
        return this;
    }

    public Sheet addDanger(int icon, String t, Runnable r) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.danger = true;
        it.action = r;
        items.add(it);
        return this;
    }

    public void show(final OverlayHost host) {
        if (host.isOpen()) return;
        dismissing = false;
        final MorphFrame morph = new MorphFrame(ctx);
        morph.pivot(0.5f, 1f);
        final RoundBox panel = new RoundBox(ctx);
        panel.setFill(Palette.CARD);
        panel.corners(26, 26, 0, 0);
        panel.setShadow(Palette.SHADOW, 14);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(14), Ui.dp(14));
        panel.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        View grabber = new View(ctx) {
            protected void onDraw(Canvas canvas) {
                Paint p = Ui.fillPaint(Palette.LINE);
                RectF r = new RectF(getWidth() / 2f - Ui.dpf(18), getHeight() / 2f - Ui.dpf(2),
                        getWidth() / 2f + Ui.dpf(18), getHeight() / 2f + Ui.dpf(2));
                canvas.drawRoundRect(r, Ui.dpf(2), Ui.dpf(2), p);
            }
        };
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(18));
        col.addView(grabber, glp);

        if (title != null && title.length() > 0) {
            Label tl = new Label(ctx, title, 17f, Palette.INK, true);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.leftMargin = Ui.dp(10);
            tlp.topMargin = Ui.dp(2);
            tlp.bottomMargin = Ui.dp(subtitle == null ? 8 : 2);
            col.addView(tl, tlp);
        }
        if (subtitle != null && subtitle.length() > 0) {
            Label sl = new Label(ctx, subtitle, 11.8f, Palette.INK_3, false);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            slp.leftMargin = Ui.dp(10);
            slp.bottomMargin = Ui.dp(8);
            col.addView(sl, slp);
        }

        for (int i = 0; i < items.size(); i++) {
            final Item it = items.get(i);
            MenuRow row = new MenuRow(ctx, MenuRow.STYLE_PLAIN);
            row.title(it.title).desc(it.desc).chevron(false).checked(it.checked).danger(it.danger);
            if (it.letter != null) row.letter(it.letter, Palette.PINK_SOFT, Palette.PINK_DEEP);
            else if (it.icon != 0) row.icon(it.icon, it.danger ? 0xFFFBE9E9 : Palette.PINK_SOFT,
                    it.danger ? Palette.RED : Palette.PINK_DEEP);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dismiss(host, morph);
                    if (it.action != null) {
                        Tween.post(it.action, 150);
                    }
                }
            });
            col.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        SoftButton cancel = new SoftButton(ctx, "取消", 14.4f, Palette.INK_2, Palette.BG_DEEP);
        cancel.pad(16, 12).radius(16);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss(host, morph);
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(10);
        col.addView(cancel, clp);

        morph.addView(panel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout wrap = new FrameLayout(ctx);
        wrap.addView(morph, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        host.show(new Runnable() {
            public void run() {
                dismiss(host, morph);
            }
        });
        host.setCloseAction(new Runnable() {
            public void run() {
                dismiss(host, morph);
            }
        });
        morph.transform(0f, 0.80f, 0f, Ui.dp(26));
        host.layer().addView(wrap, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        growIn(morph);
    }

    /**
     * M3 弹出：从底部“很小”的状态快速长大并抬到位，96ms 内完成。
     * 缩放锚点在底部中心，所以视觉上不会从屏幕中央炸开。
     */
    private void growIn(final MorphFrame morph) {
        morph.post(new Runnable() {
            public void run() {
                final int h = morph.getHeight();
                if (h == 0) {
                    morph.post(this);
                    return;
                }
                final int lift = Ui.dp(26);
                Tween.of(0, 1, 96, new Tween.Listener() {
                    public void onUpdate(float v) {
                        morph.transform(v, 0.80f + 0.20f * v, 0f, (int) (lift * (1f - v)));
                    }

                    public void onEnd() {
                        morph.transform(1f, 1f, 0f, 0f);
                    }
                }).ease(Tween.DECEL).start();
            }
        });
    }

    private void dismiss(final OverlayHost host, final MorphFrame morph) {
        if (dismissing) return;
        dismissing = true;
        Tween t = Tween.of(0, 1, 120, new Tween.Listener() {
            public void onUpdate(float v) {
                morph.transform(1f - v, 1f - 0.06f * v, 0f, (int) (Ui.dp(16) * v));
            }

            public void onEnd() {
                host.hide();
            }
        });
        t.ease(Tween.ACCEL).start();
    }
}
