package com.zoffcc.applications.trifa;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Handler {
    private final Looper mLooper;

    public Handler(Looper looper) {
        this.mLooper = looper;
    }

    /**
     * Removes any pending posts of callbacks and sent messages whose 'obj' is 'token'.
     * If 'token' is null, all callbacks and messages for this Handler will be removed.
     */
    public final void removeCallbacksAndMessages(Object token) {
        mLooper.mQueue.removeIf(msg -> {
            // Only remove messages that belong to THIS handler instance
            if (msg.target != this) return false;

            // If token is null, remove everything for this handler
            if (token == null) return true;

            // Otherwise, only remove if the message's object matches the token
            return msg.obj == token;
        });
    }

    public void handleMessage(Message msg) {}

    public final void sendMessageDelayed(Message msg, long delayMillis) {
        msg.target = this;
        msg.when = System.currentTimeMillis() + delayMillis;
        mLooper.mQueue.put(msg);
    }

    /**
     * Causes the Runnable r to be added to the message queue.
     * The runnable will be run on the thread to which this handler is attached.
     */
    public final void post(Runnable r) {
        postDelayed(r, 0);
    }

    public final void postDelayed(Runnable r, long delayMillis) {
        Message msg = new Message();
        msg.callback = r;
        sendMessageDelayed(msg, delayMillis);
    }

    public static class Message implements Delayed {
        public int what;
        public Object obj;
        long when;
        Handler target;
        Runnable callback;

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(when - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.when, ((Message) o).when);
        }
    }

    public static class Looper extends Thread {
        final DelayQueue<Message> mQueue = new DelayQueue<>();
        private volatile boolean mRunning = true;

        @Override
        public void run() {
            try {
                while (mRunning) {
                    Message msg = mQueue.take();
                    if (msg.callback != null) {
                        msg.callback.run();
                    } else if (msg.target != null) {
                        msg.target.handleMessage(msg);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void quit() {
            mRunning = false;
            this.interrupt();
        }
    }
}
