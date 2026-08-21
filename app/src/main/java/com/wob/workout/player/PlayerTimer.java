package com.wob.workout.player;

import android.os.CountDownTimer;

/**
 * Wraps a single {@link CountDownTimer}, handling the cancel-and-recreate-with-remaining-
 * time dance Android's CountDownTimer requires for pause/resume. One instance is reused
 * across both the exercise countdown and the 3-second transition countdown — never more
 * than one running at once in the player's state machine. See wayfinder ticket #4.
 */
public class PlayerTimer {

    public interface Listener {
        void onTick(long millisUntilFinished);
        void onFinish();
    }

    private final Listener listener;
    private CountDownTimer timer;
    private long millisLeft;
    private boolean running;

    public PlayerTimer(Listener listener) {
        this.listener = listener;
    }

    public void start(long millis) {
        cancel();
        millisLeft = millis;
        running = true;
        timer = newTimer(millisLeft);
        timer.start();
    }

    public void pause() {
        if (!running) return;
        if (timer != null) timer.cancel();
        running = false;
    }

    public void resume() {
        if (running) return;
        running = true;
        timer = newTimer(millisLeft);
        timer.start();
    }

    public void cancel() {
        if (timer != null) timer.cancel();
        timer = null;
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public long getMillisLeft() {
        return millisLeft;
    }

    private CountDownTimer newTimer(long millis) {
        return new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisLeft = millisUntilFinished;
                listener.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                millisLeft = 0;
                running = false;
                listener.onFinish();
            }
        };
    }
}
