package com.chatyue.app.ui.m3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Theme;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;

/** 经典主题外观页：4 列主题卡 + 字号步进 + M3 实时预览。 */
public class M3AppearancePage extends Page {

    private LinearLayout content;

    public M3AppearancePage(MainActivity a) {
        super(a);
        setBackgroundColor(M3.BG);

        M3TopBar bar = new M3TopBar(a, "外观", "经典主题 · Material 3", Icons.BACK, 0);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(70)));

        BounceScrollView scroll = new BounceScrollView(a);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(18), Ui.dp(2), Ui.dp(18), Ui.dp(30));
        scroll.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        slp.topMargin = Ui.dp(70);
        addView(scroll, slp);

        build();
    }

    private void build() {
        content.removeAllViews();
        Label hi = new Label(act, "换个颜色，也换套页面", 25f, M3.ON_SURFACE, true);
        content.addView(hi, lp(4, 12, 4, 0));
        Label sub = new Label(act, "「经典」进入独立 M3，其余回到默认页面。",
                12.2f, M3.ON_SURFACE_2, false);
        content.addView(sub, lp(4, 6, 4, 0));

        M3Card themeCard = new M3Card(act);
        themeCard.setRadius(26);
        themeCard.pad(8, 10, 8, 8);
        ThemeM3Grid grid = new ThemeM3Grid(act);
        grid.selected(Prefs.theme(act)).listener(new ThemeM3Grid.Listener() {
            public void onPick(int index) {
                applyTheme(index);
            }
        });
        themeCard.addView(grid, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        content.addView(themeCard, lp(0, 18, 0, 0));

        content.addView(section("文字"), lp(4, 22, 4, 8));
        M3Card fontCard = new M3Card(act);
        fontCard.setRadius(24);
        fontCard.pad(0, 2, 0, 2);
        M3Row fontRow = new M3Row(act, false);
        fontRow.icon(Icons.EDIT);
        fontRow.title("字体大小").desc("影响消息正文与界面文字");
        fontRow.value(Prefs.fontPercent(act) + "%");
        LinearLayout stepper = new LinearLayout(act);
        stepper.setOrientation(LinearLayout.HORIZONTAL);
        stepper.setGravity(Gravity.CENTER_VERTICAL);
        M3Button minus = new M3Button(act, "−", M3Button.OUTLINED);
        minus.height(36).padH(12);
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setFont(Prefs.fontPercent(act) - 6);
            }
        });
        M3Button plus = new M3Button(act, "+", M3Button.OUTLINED);
        plus.height(36).padH(12);
        plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setFont(Prefs.fontPercent(act) + 6);
            }
        });
        stepper.addView(minus, new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(36)));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(Ui.dp(6), Ui.dp(1));
        stepper.addView(new View(act), gap);
        stepper.addView(plus, new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(36)));
        fontRow.trailing(stepper);
        fontCard.addView(fontRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(fontCard, lp(0, 0, 0, 0));

        content.addView(section("预览"), lp(4, 22, 4, 8));
        content.addView(previewCard(), lp(0, 0, 0, 0));

        com.chatyue.app.ui.md.MdText note = new com.chatyue.app.ui.md.MdText(act,
                "经典主题的圆角、描边、状态层、弹出动效都按 Material 3 重画；默认主题仍保留原来的奶油手绘风。",
                Ui.textPaint(M3.ON_SURFACE_3, 10.8f, false), 0, 0, 3f);
        content.addView(note, lp(6, 16, 6, 0));
        M3.reveal(content);
    }

    private View previewCard() {
        M3Card card = new M3Card(act);
        card.setRadius(26);
        card.pad(18, 18, 18, 18);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);

        Label who = new Label(act, "你", 10f, M3.ON_SURFACE_3, false);
        who.setAlign(Label.RIGHT);
        col.addView(who, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        RoundBox bubble = new RoundBox(act);
        bubble.setFill(M3.PRIMARY_CONTAINER);
        bubble.corners(20, 20, 6, 20);
        bubble.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
        Label ut = new Label(act, "这个经典主题挺好看。", 14f, M3.ON_PRIMARY_CONTAINER, false);
        bubble.addView(ut);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.RIGHT;
        blp.topMargin = Ui.dp(6);
        col.addView(bubble, blp);

        Label bot = new Label(act, "ChatYUE", 11.4f, M3.PRIMARY_DEEP, true);
        LinearLayout.LayoutParams botlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        botlp.topMargin = Ui.dp(14);
        col.addView(bot, botlp);

        com.chatyue.app.ui.md.MdText body = new com.chatyue.app.ui.md.MdText(act,
                "它的页面、导航、面板都是另一套 M3 实现，不是简单换色。",
                Ui.textPaint(M3.ON_SURFACE, 13.8f, false), 0, 0, 4f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = Ui.dp(6);
        col.addView(body, bodyLp);

        card.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void setFont(int value) {
        int v = Ui.clamp(value, 85, 130);
        Prefs.setFontPercent(act, v);
        build();
        act.toast("字号 " + v + "%（重开对话生效）");
    }

    private void applyTheme(int index) {
        Prefs.setTheme(act, index);
        Palette.apply(index);
        act.toast("已切换：" + Theme.name(index));
        act.rebuildAfterTheme();
    }

    private View section(String title) {
        Label l = new Label(act, title, 15.4f, M3.ON_SURFACE, true);
        l.setPadding(Ui.dp(4), 0, 0, 0);
        return l;
    }

    private LinearLayout.LayoutParams lp(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = Ui.dp(left);
        p.rightMargin = Ui.dp(right);
        p.topMargin = Ui.dp(top);
        p.bottomMargin = Ui.dp(bottom);
        return p;
    }

    /** 4 列主题卡：选中项描边 + 勾，点按即时切换。 */
    private static final class ThemeM3Grid extends View {
        interface Listener {
            void onPick(int index);
        }

        private static final int COLS = 4;
        private int selected;
        private Listener listener;
        private final Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint check = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path tick = new Path();
        private final TextPaint label = Ui.textPaint(M3.ON_SURFACE_2, 10.6f, false);

        ThemeM3Grid(Context c) {
            super(c);
            label.setTextAlign(Paint.Align.CENTER);
            setClickable(true);
        }

        ThemeM3Grid selected(int index) {
            selected = index;
            invalidate();
            return this;
        }

        ThemeM3Grid listener(Listener l) {
            listener = l;
            return this;
        }

        private int rows() {
            return (Theme.COUNT + COLS - 1) / COLS;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Ui.dp(rows() * 92));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            float cellW = w / (float) COLS;
            float cellH = Ui.dpf(92);
            for (int i = 0; i < Theme.COUNT; i++) {
                int row = i / COLS;
                int col = i % COLS;
                float cx = cellW * col + cellW / 2f;
                float cy = cellH * row + Ui.dpf(30);
                float r = Ui.dpf(20);

                circle.setColor(Theme.accent(i));
                canvas.drawCircle(cx, cy, r, circle);

                if (i == selected) {
                    ring.setStyle(Paint.Style.STROKE);
                    ring.setStrokeWidth(Math.max(1.8f, Ui.dpf(2f)));
                    ring.setColor(M3.PRIMARY);
                    canvas.drawCircle(cx, cy, r + Ui.dpf(5), ring);
                    check.setColor(0xFFFFFFFF);
                    check.setStyle(Paint.Style.STROKE);
                    check.setStrokeWidth(Ui.dpf(2f));
                    tick.reset();
                    tick.moveTo(cx - Ui.dpf(5.5f), cy + Ui.dpf(0.5f));
                    tick.lineTo(cx - Ui.dpf(1.2f), cy + Ui.dpf(4.6f));
                    tick.lineTo(cx + Ui.dpf(6f), cy - Ui.dpf(4.4f));
                    canvas.drawPath(tick, check);
                }

                label.setColor(i == selected ? M3.ON_SURFACE : M3.ON_SURFACE_3);
                float ty = cy + r + Ui.dpf(20);
                canvas.drawText(Theme.name(i), cx, Ui.baselineCentered(label, ty), label);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float cellW = getWidth() / (float) COLS;
                float cellH = Ui.dpf(92);
                int col = (int) (e.getX() / cellW);
                int row = (int) (e.getY() / cellH);
                int index = row * COLS + col;
                if (col >= 0 && col < COLS && row >= 0 && index < Theme.COUNT) {
                    selected = index;
                    invalidate();
                    if (listener != null) listener.onPick(index);
                    performClick();
                }
            }
            return true;
        }
    }
}
