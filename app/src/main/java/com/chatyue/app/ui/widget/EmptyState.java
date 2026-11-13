package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;

import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 空状态：圆形图标 + 标题 + 说明，用于列表无数据时，替代系统空列表的空白感。 */
public class EmptyState extends View {

    private final int icon;
    private final String title;
    private final String desc;
    private final float heightDp;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = Ui.textPaint(Palette.INK, 15f, true);
    private final TextPaint descPaint = Ui.textPaint(Palette.INK_3, 12.4f, false);

    public EmptyState(Context c, int icon, String title, String desc) {
        this(c, icon, title, desc, 210);
    }

    public EmptyState(Context c, int icon, String title, String desc, float heightDp) {
        super(c);
        this.icon = icon;
        this.title = title == null ? "" : title;
        this.desc = desc == null ? "" : desc;
        this.heightDp = heightDp;
        titlePaint.setTextAlign(Paint.Align.CENTER);
        descPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Ui.dp(heightDp));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f - Ui.dpf(18);
        float r = Ui.dpf(34);

        bg.setColor(Palette.PINK_SOFT);
        canvas.drawCircle(cx, cy, r, bg);
        Icons.draw(canvas, icon, cx, cy, Ui.dpf(26), Palette.PINK_DEEP, 1.7f);

        float ty = cy + r + Ui.dpf(30);
        canvas.drawText(title, cx, Ui.baselineCentered(titlePaint, ty), titlePaint);

        String[] lines = wrap(desc, w - Ui.dp(72));
        float y = cy + r + Ui.dpf(52);
        for (int i = 0; i < lines.length && i < 3; i++) {
            canvas.drawText(lines[i], cx, y, descPaint);
            y += Ui.dpf(19);
        }
    }

    private String[] wrap(String text, float maxWidth) {
        if (text.length() == 0) return new String[0];
        String[] raw = text.split("\n");
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (int i = 0; i < raw.length; i++) {
            String line = raw[i];
            while (descPaint.measureText(line) > maxWidth && line.length() > 1) {
                int cut = line.length();
                while (cut > 1 && descPaint.measureText(line.substring(0, cut)) > maxWidth) cut--;
                out.add(line.substring(0, cut));
                line = line.substring(cut);
            }
            out.add(line);
        }
        return out.toArray(new String[out.size()]);
    }
}
