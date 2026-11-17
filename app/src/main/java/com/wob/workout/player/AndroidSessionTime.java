package com.wob.workout.player;

import android.os.CountDownTimer;

/**
 * {@link SessionTime} on a real {@link CountDownTimer} and the system clock. Handles the
 * cancel-and-recreate-with-remaining-time dance Android's CountDownTimer requires for
 * pause/resume. Its callbacks arrive on the thread that started it, the main thread.
 */
public class AndroidSessionTime implements SessionTime {

    private CountDownTimer timer;
    private Listener listener;
    private long millisLeft;
    private boolean running;

    @Override
    public long now() {
        return System.currentTimeMillis();
    }

    @Override
    public void start(long millis, Listener listener) {
        cancel();
        this.listener = listener;
        millisLeft = millis;
        running = true;
        timer = newTimer(millisLeft);
        timer.start();
    }

    @Override
    public void pause() {
        if (!running) return;
        if (timer != null) timer.cancel();
        running = false;
    }

    @Override
    public void resume() {
        if (running || listener == null) return;
        running = true;
        timer = newTimer(millisLeft);
        timer.start();
    }

    @Override
    public void cancel() {
        if (timer != null) timer.cancel();
        timer = null;
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public long millisLeft() {
        return millisLeft;
    }

    private CountDownTimer newTimer(long millis) {
        Listener target = listener;
        return new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisLeft = millisUntilFinished;
                target.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                millisLeft = 0;
                running = false;
                target.onFinish();
            }
        };
    }
}
