package com.example.cs205;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

public class Timer {
    public interface TimerListener {
        void onTick(long millisUntilFinished);
        void onFinish();
    }

    private CountDownTimer timer;
    private final long durationMillis;
    private final long intervalMillis;
    private final TimerListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startTimeMillis;

    public Timer(long durationMillis, long intervalMillis, TimerListener listener) {
        this.durationMillis = durationMillis;
        this.intervalMillis = intervalMillis;
        this.listener = listener;
    }

    public void start() {
        stop(); // Cancel any existing timer
        startTimeMillis = System.currentTimeMillis();

        timer = new CountDownTimer(durationMillis, intervalMillis) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedTime = System.currentTimeMillis() - startTimeMillis;
                long remainingTime = durationMillis - elapsedTime;
                final long adjustedRemainingTime = Math.max(remainingTime, 0); // Ensure no negative values
                handler.post(() -> listener.onTick(adjustedRemainingTime));
            }

            @Override
            public void onFinish() {
                handler.post(() -> listener.onFinish());
            }
        }.start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public boolean isRunning() {
        return timer != null;
    }
}