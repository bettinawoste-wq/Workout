package com.wob.workout.player;

import android.os.CountDownTimer;

public class WorkoutTimer {

    public interface Listener {
        void onTick(long millisLeft);
        void onFinish();
    }

    private final Listener listener;
    private CountDownTimer timer;
    private long timeLeft;
    private boolean running;

    public WorkoutTimer(Listener listener) {
        this.listener = listener;
    }

    public void start(int seconds) {
        timeLeft = seconds * 1000L;
        running = true;
        createTimer(timeLeft);
    }

    public void pause() {
        if (timer != null) timer.cancel();
        running = false;
    }

    public void resume() {
        running = true;
        createTimer(timeLeft);
    }

    public void cancel() {
        if (timer != null) timer.cancel();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public long getTimeLeft() {
        return timeLeft;
    }

    private void createTimer(long millis) {
        timer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                listener.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                running = false;
                listener.onFinish();
            }
        }.start();
    }
}