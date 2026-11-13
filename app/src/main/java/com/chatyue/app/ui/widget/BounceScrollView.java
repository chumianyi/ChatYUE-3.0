package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;

/**
 * 带边界回弹的滚动容器。
 *
 * 低版本的 ScrollView 没有 overScrollBy 回调（API 9 才有），因此回弹不用 overscroll 机制，
 * 而是在 dispatchDraw 里对画布做一次位移，配合自己写的弹簧补间收尾，行为在各版本一致。
 */
public class BounceScrollView extends ScrollView {

    private float bounce;
    private float lastY = -1f;
    private float maxBounce;
    private Tween spring;
    private boolean bounceEnabled = true;

    public BounceScrollView(Context c) {
        super(c);
        maxBounce = Ui.dpf(86);
        setVerticalScrollBarEnabled(false);
        setFillViewport(false);
    }

    public void setBounceEnabled(boolean e) {
        bounceEnabled = e;
    }

    public boolean isNearBottom() {
        if (getChildCount() == 0) return true;
        View child = getChildAt(0);
        return getScrollY() + getHeight() >= child.getHeight() - Ui.dp(80);
    }

    public void scrollToBottomNow() {
        if (getChildCount() == 0) return;
        View child = getChildAt(0);
        int y = Math.max(0, child.getHeight() - getHeight());
        scrollTo(0, y);
    }

    public void scrollToBottomSoon() {
        post(new Runnable() {
            public void run() {
                scrollToBottomNow();
            }
        });
    }

    private boolean atTop() {
        return getScrollY() <= 0;
    }

    private boolean atBottom() {
        if (getChildCount() == 0) return true;
        View child = getChildAt(0);
        return getScrollY() + getHeight() >= child.getHeight() - 1;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction() & 0xff;
        if (!bounceEnabled) {
            return super.dispatchTouchEvent(ev);
        }
        if (action == MotionEvent.ACTION_DOWN) {
            lastY = ev.getY();
            if (spring != null) spring.cancel();
        } else if (action == MotionEvent.ACTION_MOVE) {
            float y = ev.getY();
            if (lastY >= 0) {
                float dy = y - lastY;
                boolean pullTop = dy > 0 && atTop();
                boolean pullBottom = dy < 0 && atBottom();
                if (pullTop || pullBottom || bounce != 0f) {
                    float next = bounce + dy * 0.45f;
                    if (next > maxBounce) next = maxBounce;
                    if (next < -maxBounce) next = -maxBounce;
                    bounce = next;
                    invalidate();
                }
            }
            lastY = y;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            lastY = -1f;
            springBack();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void springBack() {
        if (bounce == 0f) return;
        if (spring != null) spring.cancel();
        spring = Tween.of(bounce, 0f, 340, new Tween.Listener() {
            public void onUpdate(float v) {
                bounce = v;
                invalidate();
            }

            public void onEnd() {
                bounce = 0f;
                invalidate();
            }
        }).ease(Tween.SPRING);
        spring.start();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (bounce == 0f) {
            super.dispatchDraw(canvas);
            return;
        }
        canvas.save();
        canvas.translate(0, bounce);
        super.dispatchDraw(canvas);
        canvas.restore();
    }
}
