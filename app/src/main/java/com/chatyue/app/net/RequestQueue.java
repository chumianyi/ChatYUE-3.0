package com.chatyue.app.net;

import android.os.Handler;
import android.os.Looper;

import java.util.LinkedList;

/**
 * 自建请求队列。
 *
 * 低版本没有现成可用的线程池封装（也没有生命周期管理组件），这里用一个常驻工作线程
 * 加链表实现串行队列；所有 UI 回调用主线程 Handler 转发。
 */
public final class RequestQueue {

    private static RequestQueue sInstance;

    private final LinkedList<Runnable> queue = new LinkedList<Runnable>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Thread worker;
    private volatile boolean running = true;

    private RequestQueue() {
        startWorker();
    }

    public static synchronized RequestQueue get() {
        if (sInstance == null) sInstance = new RequestQueue();
        return sInstance;
    }

    public Handler main() {
        return main;
    }

    public void postMain(Runnable r) {
        main.post(r);
    }

    public void postMainDelayed(Runnable r, long delay) {
        main.postDelayed(r, delay);
    }

    private void startWorker() {
        worker = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    Runnable task = null;
                    synchronized (queue) {
                        if (queue.isEmpty()) {
                            try {
                                queue.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                        if (!queue.isEmpty()) task = queue.removeFirst();
                    }
                    if (task != null) {
                        try {
                            task.run();
                        } catch (Throwable t) {
                            // 任务异常不能拖垮整个队列
                            t.printStackTrace();
                        }
                    }
                }
            }
        }, "chatyue-net");
        worker.setPriority(Thread.NORM_PRIORITY - 1);
        worker.start();
    }

    public void submit(Runnable task) {
        if (task == null) return;
        synchronized (queue) {
            queue.addLast(task);
            queue.notifyAll();
        }
    }

    public void cancelAll() {
        synchronized (queue) {
            queue.clear();
        }
    }

    public void shutdown() {
        running = false;
        synchronized (queue) {
            queue.notifyAll();
        }
    }
}
