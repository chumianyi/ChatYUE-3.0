package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.overlay.OverlayHost;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MorphFrame;
import com.chatyue.app.ui.widget.RoundBox;

import java.util.ArrayList;

/**
 * M3 底部面板：抓杆 + 大标题 + 图标行 + 右侧「取消」文字按钮。
 * 弹出时从底部 80% 大小长到 100%，96ms 完成；关闭时反向收缩。
 */
public class M3Sheet {

    public static class Item {
        public int icon;
        public String title = "";
        public String desc = "";
        public boolean danger;
        public boolean checked;
        public boolean dividerBefore;
        public Runnable action;
        public View trailing;
    }

    private final Context ctx;
    private final ArrayList<Item> items = new ArrayList<Item>();
    private String title;
    private String subtitle;
    private boolean dismissing;

    public M3Sheet(Context c) {
        ctx = c;
    }

    public M3Sheet title(String t) {
        title = t;
        return this;
    }

    public M3Sheet subtitle(String s) {
        subtitle = s;
        return this;
    }

    public M3Sheet add(int icon, String t, Runnable r) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.action = r;
        items.add(it);
        return this;
    }

    public M3Sheet addDesc(int icon, String t, String d, Runnable r) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.desc = d;
        it.action = r;
        items.add(it);
        return this;
    }

    public M3Sheet addChecked(int icon, String t, boolean checked, Runnable r) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.checked = checked;
        it.action = r;
        items.add(it);
        return this;
    }

    public M3Sheet addSwitch(int icon, String t, String d, boolean checked, M3Switch.Listener l) {
        Item it = new Item();
        it.icon = icon;
        it.title = t;
        it.desc = d;
        final M3Switch sw = new M3Switch(ctx);
        sw.checked(checked).listener(l);
        it.trailing = sw;
        items.add(it);
        return this;
    }

    public M3Sheet divider() {
        if (items.size() > 0) items.get(items.size() - 1).dividerBefore = true;
        return this;
    }

    public M3Sheet addDanger(int icon, String t, Runnable r) {
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

        RoundBox panel = new RoundBox(ctx);
        panel.setFill(M3.SHEET);
        panel.setRadius(28);
        panel.corners(30, 30, 0, 0);
        panel.setShadow(0x332A1B22, 18);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(18), Ui.dp(10), Ui.dp(18), Ui.dp(16));
        panel.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        View grabber = new View(ctx) {
            protected void onDraw(Canvas canvas) {
                Paint p = Ui.fillPaint(M3.OUTLINE_VARIANT);
                RectF r = new RectF(getWidth() / 2f - Ui.dpf(16), getHeight() / 2f - Ui.dpf(2),
                        getWidth() / 2f + Ui.dpf(16), getHeight() / 2f + Ui.dpf(2));
                canvas.drawRoundRect(r, Ui.dpf(2), Ui.dpf(2), p);
            }
        };
        col.addView(grabber, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(22)));

        if (title != null && title.length() > 0) {
            Label tl = new Label(ctx, title, 23f, M3.ON_SURFACE, true);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.leftMargin = Ui.dp(6);
            tlp.bottomMargin = Ui.dp(4);
            col.addView(tl, tlp);
        }
        if (subtitle != null && subtitle.length() > 0) {
            Label sl = new Label(ctx, subtitle, 12.4f, M3.ON_SURFACE_3, false);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            slp.leftMargin = Ui.dp(6);
            slp.bottomMargin = Ui.dp(8);
            col.addView(sl, slp);
        }

        for (int i = 0; i < items.size(); i++) {
            final Item it = items.get(i);
            M3Row row = new M3Row(ctx, true);
            row.title(it.title).desc(it.desc).danger(it.danger).checked(it.checked);
            row.icon(it.icon);
            row.divider(it.dividerBefore);
            row.dividerInset(false);
            if (it.trailing != null) row.trailing(it.trailing);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (it.trailing instanceof M3Switch) {
                        ((M3Switch) it.trailing).toggle();
                        return;
                    }
                    if (it.trailing != null) return;
                    dismiss(host, morph);
                    if (it.action != null) Tween.post(it.action, 130);
                }
            });
            col.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        // 取消：右下角文字按钮，和截图一致
        LinearLayout cancelRow = new LinearLayout(ctx);
        cancelRow.setOrientation(LinearLayout.HORIZONTAL);
        cancelRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        M3Button cancel = new M3Button(ctx, "取消", M3Button.TEXT);
        cancel.height(44);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss(host, morph);
            }
        });
        cancelRow.addView(cancel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(44)));
        LinearLayout.LayoutParams crlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(52));
        crlp.topMargin = Ui.dp(4);
        col.addView(cancelRow, crlp);

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
        morph.transform(0f, 0.80f, 0f, Ui.dp(22));
        host.layer().addView(wrap, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        growIn(morph);
    }

    private void growIn(final MorphFrame morph) {
        morph.post(new Runnable() {
            public void run() {
                final int h = morph.getHeight();
                if (h == 0) {
                    morph.post(this);
                    return;
                }
                final int lift = Ui.dp(22);
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
        Tween t = Tween.of(0, 1, 118, new Tween.Listener() {
            public void onUpdate(float v) {
                morph.transform(1f - v, 1f - 0.07f * v, 0f, (int) (Ui.dp(14) * v));
            }

            public void onEnd() {
                host.hide();
            }
        });
        t.ease(Tween.ACCEL).start();
    }
}
