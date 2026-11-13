package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Session;
import com.chatyue.app.util.TimeUtil;

/** 会话列表行。左侧状态圆点 + 标题 / 摘要 + 时间 + 删除热区。 */
public class SessionRow extends ViewGroup {

    public interface Listener {
        void onClick(Session s);

        void onDelete(Session s);

        void onLongClick(Session s);
    }

    private Session session;
    private boolean active;
    private boolean last;
    private boolean pressed;
    private boolean deletePressed;
    private boolean longFired;

    private final TextPaint titlePaint = Ui.textPaint(Palette.INK, 15f, true);
    private final TextPaint previewPaint = Ui.textPaint(Palette.INK_3, 12.2f, false);
    private final TextPaint timePaint = Ui.textPaint(Palette.INK_4, 11f, false);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = Ui.fillPaint(Palette.LINE_2);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;

    public SessionRow(Context c, Session s, boolean active, boolean last) {
        super(c);
        setWillNotDraw(false);
        setSession(s, active, last);
    }

    public void setSession(Session s, boolean activeRow, boolean lastRow) {
        session = s;
        active = activeRow;
        last = lastRow;
        invalidate();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) w = Ui.screenWidth() - Ui.dp(32);
        setMeasuredDimension(w, Ui.dp(70));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    private boolean inDelete(float x, float y) {
        return x > getWidth() - Ui.dp(46) && x < getWidth() - Ui.dp(8);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        bgPaint.setColor(pressed || deletePressed ? Palette.BG_DEEP : Palette.CARD);
        canvas.drawRoundRect(new android.graphics.RectF(0, 0, w, h), Ui.dpf(16), Ui.dpf(16), bgPaint);

        dotPaint.setColor(active ? Palette.PINK : Palette.LINE);
        canvas.drawCircle(Ui.dp(24), h / 2f, Ui.dpf(active ? 4.5f : 3.5f), dotPaint);

        float tx = Ui.dp(42);
        float rightLimit = w - Ui.dp(50);
        canvas.drawText(Ui.ellipsize(titlePaint, session.title, rightLimit - tx),
                tx, h / 2f - Ui.dpf(4), titlePaint);

        String preview = session.preview();
        canvas.drawText(Ui.ellipsize(previewPaint, preview, rightLimit - tx),
                tx, h / 2f + Ui.dpf(15), previewPaint);

        String time = TimeUtil.friendly(session.updatedAt);
        canvas.drawText(time, w - Ui.dp(46) - timePaint.measureText(time), h / 2f - Ui.dpf(4), timePaint);

        com.chatyue.app.core.Icons.draw(canvas, com.chatyue.app.core.Icons.TRASH,
                w - Ui.dp(26), h / 2f, Ui.dpf(15),
                deletePressed ? Palette.RED : Palette.INK_4, 1.6f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                deletePressed = inDelete(x, y);
                pressed = !deletePressed;
                longFired = false;
                postDelayed(new Runnable() {
                    public void run() {
                        if (pressed && !longFired && listener != null) {
                            longFired = true;
                            listener.onLongClick(session);
                        }
                    }
                }, 520);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - getWidth() / 2f) > 0 && pressed) {
                    // 手指滑出即取消长按
                }
                return true;
            case MotionEvent.ACTION_UP:
                pressed = false;
                boolean del = deletePressed;
                deletePressed = false;
                invalidate();
                if (longFired) return true;
                if (del && inDelete(x, y)) {
                    if (listener != null) listener.onDelete(session);
                } else if (listener != null) {
                    listener.onClick(session);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                deletePressed = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }
}
