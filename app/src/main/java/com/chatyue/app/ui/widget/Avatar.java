package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/** 圆形头像：支持位图（BitmapShader 圆形裁切）与文字占位两种模式。 */
public class Avatar extends View {

    private Bitmap bitmap;
    private BitmapShader shader;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint letterPaint;
    private String letter = "Y";
    private int bg = Palette.PINK_SOFT;
    private int fg = Palette.PINK_DEEP;
    private float ringW = 0;
    private int ringColor = 0xFFFFFFFF;

    public Avatar(Context c) {
        super(c);
        letterPaint = Ui.textPaint(fg, 14, true);
        letterPaint.setTextAlign(Paint.Align.CENTER);
    }

    public Avatar setBitmap(Bitmap b) {
        bitmap = b;
        if (b != null) {
            shader = new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        } else {
            shader = null;
        }
        invalidate();
        return this;
    }

    public void setLetter(String l) {
        letter = l == null ? "" : l;
        invalidate();
    }

    public void setColors(int background, int foreground) {
        bg = background;
        fg = foreground;
        letterPaint.setColor(foreground);
        invalidate();
    }

    public void setRing(int color, float widthDp) {
        ringColor = color;
        ringW = widthDp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy);
        if (ringW > 0) {
            ringPaint.setColor(ringColor);
            canvas.drawCircle(cx, cy, r, ringPaint);
            r -= Ui.dpf(ringW);
        }
        if (shader != null && bitmap != null) {
            Matrix m = new Matrix();
            float scale = Math.max((r * 2) / bitmap.getWidth(), (r * 2) / bitmap.getHeight());
            m.setScale(scale, scale);
            m.postTranslate(cx - bitmap.getWidth() * scale / 2f, cy - bitmap.getHeight() * scale / 2f);
            shader.setLocalMatrix(m);
            paint.setShader(shader);
            canvas.drawCircle(cx, cy, r, paint);
            paint.setShader(null);
        } else {
            bgPaint.setColor(bg);
            canvas.drawCircle(cx, cy, r, bgPaint);
            letterPaint.setTextSize(r * 0.85f);
            float base = cy - (letterPaint.getFontMetricsInt().ascent + letterPaint.getFontMetricsInt().descent) / 2f;
            canvas.drawText(letter, cx, base, letterPaint);
        }
    }
}
