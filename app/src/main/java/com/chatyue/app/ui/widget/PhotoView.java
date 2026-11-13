package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import com.chatyue.app.core.Ui;

/** 消息里的图片缩略图，圆角裁切，中心裁切填充。 */
public class PhotoView extends View {

    private Bitmap bitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private final Rect src = new Rect();

    public PhotoView(Context c) {
        super(c);
        paint.setFilterBitmap(true);
    }

    public void setPath(String path) {
        if (path == null) return;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            bitmap = BitmapFactory.decodeFile(path, o);
        } catch (Exception e) {
            bitmap = null;
        } catch (OutOfMemoryError e) {
            bitmap = null;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w = Ui.dp(200);
        }
        int h = (int) (w * 0.68f);
        if (bitmap != null) {
            h = (int) (w * bitmap.getHeight() / (float) Math.max(1, bitmap.getWidth()));
            h = Math.min(h, Ui.dp(280));
            h = Math.max(h, Ui.dp(90));
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        float r = Ui.dpf(12);
        clip.reset();
        clip.addRoundRect(rect, r, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        if (bitmap != null) {
            float scale = Math.max(getWidth() / (float) bitmap.getWidth(),
                    getHeight() / (float) bitmap.getHeight());
            float dw = bitmap.getWidth() * scale;
            float dh = bitmap.getHeight() * scale;
            float left = (getWidth() - dw) / 2f;
            float top = (getHeight() - dh) / 2f;
            src.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawBitmap(bitmap, src, new RectF(left, top, left + dw, top + dh), paint);
        } else {
            paint.setColor(0xFFE7E2E6);
            canvas.drawRect(rect, paint);
        }
        canvas.restore();
    }
}
