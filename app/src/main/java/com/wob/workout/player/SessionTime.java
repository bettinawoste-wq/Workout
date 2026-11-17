package com.wob.workout.player;

/**
 * The time a {@link WorkoutSessionEngine} runs on: one countdown at a time, which can pause
 * and resume, and the clock its exercise sessions are timed by. {@link AndroidSessionTime} in
 * the app; tests use a fake that only moves when told to (ADR 0005).
 */
public interface SessionTime {

    interface Listener {
        /** Roughly once a second while the countdown runs. */
        void onTick(long millisLeft);

        void onFinish();
    }

    /** Wall-clock time in milliseconds. */
    long now();

    /** Starts a countdown of {@code millis}, replacing any running one. */
    void start(long millis, Listener listener);

    void pause();

    void resume();

    void cancel();

    boolean isRunning();

    /** What is left of the countdown, as of its last tick or pause. */
    long millisLeft();
}
