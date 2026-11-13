package com.chatyue.app.core;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;

/**
 * 自绘体系里的补间动画器。
 *
 * 低版本既没有属性动画框架，也没有 ViewPropertyAnimator，所有位移 / 透明度 / 缩放
 * 统一由这里驱动：基于 Handler 每帧回调，把 0..1 的进度交给调用方自行换算。
 */
public final class Tween {

    public static final int LINEAR = 0;
    public static final int DECEL = 1;
    public static final int ACCEL = 2;
    public static final int STANDARD = 3;
    public static final int OVERSHOOT = 4;
    public static final int SPRING = 5;

    public interface Listener {
        void onUpdate(float value);

        void onEnd();
    }

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private float from;
    private float to;
    private int duration = 240;
    private int delay = 0;
    private int ease = STANDARD;
    private Listener listener;
    private long startAt;
    private boolean running;
    private boolean cancelled;
    private Runnable ticker;

    public static Tween of(float from, float to, int duration, Listener l) {
        Tween t = new Tween();
        t.from = from;
        t.to = to;
        t.duration = Math.max(1, duration);
        t.listener = l;
        return t;
    }

    public static Tween of(int duration, Listener l) {
        return of(0f, 1f, duration, l);
    }

    public Tween delay(int d) {
        this.delay = Math.max(0, d);
        return this;
    }

    public Tween ease(int e) {
        this.ease = e;
        return this;
    }

    public Tween start() {
        if (running) return this;
        running = true;
        cancelled = false;
        startAt = SystemClock.uptimeMillis() + delay;
        ticker = new Runnable() {
            public void run() {
                tick();
            }
        };
        HANDLER.postDelayed(ticker, delay);
        return this;
    }

    public void cancel() {
        cancelled = true;
        running = false;
        if (ticker != null) HANDLER.removeCallbacks(ticker);
    }

    public boolean isRunning() {
        return running;
    }

    private void tick() {
        if (cancelled || listener == null) return;
        long now = SystemClock.uptimeMillis();
        float raw = (now - startAt) / (float) duration;
        if (raw < 0f) raw = 0f;
        boolean done = raw >= 1f;
        if (done) raw = 1f;
        float v = from + (to - from) * map(raw);
        listener.onUpdate(v);
        if (done) {
            running = false;
            listener.onEnd();
        } else {
            long next = startAt + (long) ((raw + 0.001f) * duration) + 8;
            long wait = Math.max(8, next - SystemClock.uptimeMillis());
            HANDLER.postDelayed(ticker, wait);
        }
    }

    private float map(float t) {
        switch (ease) {
            case LINEAR:
                return t;
            case DECEL:
                return 1f - (1f - t) * (1f - t);
            case ACCEL:
                return t * t;
            case OVERSHOOT: {
                float s = 1.70158f;
                float u = t - 1f;
                return u * u * ((s + 1f) * u + s) + 1f;
            }
            case SPRING: {
                // 阻尼正弦，收尾回弹一次
                float p = 1f - t;
                return 1f - (float) (Math.cos(t * Math.PI * 2.2) * p * p * p);
            }
            case STANDARD:
            default: {
                // 近似 Material 的 cubic-bezier(0.2, 0, 0, 1)
                float u = t - 1f;
                return 1f + u * u * u;
            }
        }
    }

    /** 一次性延迟任务（避免各处重复 new Handler）。 */
    public static void post(Runnable r, int delayMs) {
        HANDLER.postDelayed(r, Math.max(0, delayMs));
    }

    public static void postNow(Runnable r) {
        HANDLER.post(r);
    }

    public static void remove(Runnable r) {
        HANDLER.removeCallbacks(r);
    }

    private static final ArrayList<Tween> ACTIVE = new ArrayList<Tween>();

    public static void stopAll() {
        for (int i = 0; i < ACTIVE.size(); i++) {
            ACTIVE.get(i).cancel();
        }
        ACTIVE.clear();
    }
}
