package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;
import android.widget.LinearLayout;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.md.MdBlock;
import com.chatyue.app.md.MdParser;
import com.chatyue.app.ui.md.MdText;

import java.util.ArrayList;

/**
 * 思考过程卡片：模型返回 reasoning_content 时展示，可折叠。
 * 与参考稿里那张粉色「心情 / Sparks」卡片同一套视觉语言。
 */
public class ThinkingView extends RoundBox {

    private final Label header;
    private final MdText body;
    private final View chevron;
    private boolean expanded = true;
    private boolean streaming = true;
    private String text = "";
    private String title = "思考过程";

    public ThinkingView(Context c) {
        super(c);
        setFill(Palette.PINK_TINT);
        setStroke(Palette.PINK_LINE, 0.8f);
        setRadius(12);
        setPadding(Ui.dp(11), Ui.dp(9), Ui.dp(11), Ui.dp(9));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.FILL_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header = new Label(c, "思考过程", 11.2f, Palette.PINK_DEEP, true);
        head.addView(header, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final TextPaint tp = Ui.textPaint(Palette.PINK_DEEP, 11f, false);
        chevron = new View(c) {
            protected void onDraw(Canvas canvas) {
                Icons.draw(canvas, expanded ? Icons.CHEVRON_UP : Icons.CHEVRON_DOWN,
                        getWidth() / 2f, getHeight() / 2f, Ui.dpf(14), Palette.PINK_DEEP, 1.8f);
            }
        };
        head.addView(chevron, new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18)));
        col.addView(head);

        body = new MdText(c, "", Ui.textPaint(Palette.INK_2, 12.4f, false), 0, Ui.dp(4), 3.2f);
        col.addView(body, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        setOnClickListener(new OnClickListener() {
            public void onClick(android.view.View v) {
                expanded = !expanded;
                body.setVisibility(expanded ? VISIBLE : GONE);
                chevron.invalidate();
            }
        });
    }

    /** 正文开始输出后自动收起，符合主流 AI 应用的习惯。 */
    public void collapse() {
        expanded = false;
        body.setVisibility(GONE);
        chevron.invalidate();
    }

    public void setTitle(String t) {
        title = t == null || t.length() == 0 ? "思考过程" : t;
        if (!streaming) header.setText(title);
    }

    public void setStreaming(boolean s) {
        streaming = s;
        if (!s) {
            header.setText(title);
        } else {
            header.setText("任务过程".equals(title) ? "正在执行任务…" : "正在思考…");
        }
    }

    public void setReasoning(String reasoning) {
        text = reasoning == null ? "" : reasoning;
        ArrayList<MdBlock> blocks = MdParser.parse(text);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            MdBlock b = blocks.get(i);
            if (b.type == MdBlock.CODE) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(b.text);
        }
        body.setText(sb.toString());
    }

    public String getText() {
        return text;
    }
}
