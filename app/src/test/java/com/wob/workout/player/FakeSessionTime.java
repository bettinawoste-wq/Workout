package com.wob.workout.player;

/**
 * {@link SessionTime} that only moves when a test calls {@link #advance}. Ticks like
 * Android's CountDownTimer with a one-second interval: once a second after each start or
 * resume, then finishes when nothing is left.
 */
class FakeSessionTime implements SessionTime {

    private static final long TICK_MILLIS = 1000;

    private long now = 1_700_000_000_000L;
    private Listener listener;
    private long millisLeft;
    private boolean running;
    private long sinceTick;

    @Override
    public long now() {
        return now;
    }

    @Override
    public void start(long millis, Listener listener) {
        this.listener = listener;
        millisLeft = millis;
        running = true;
        sinceTick = 0;
    }

    @Override
    public void pause() {
        running = false;
    }

    @Override
    public void resume() {
        if (running || listener == null) return;
        running = true;
        sinceTick = 0;
    }

    @Override
    public void cancel() {
        running = false;
        listener = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public long millisLeft() {
        return millisLeft;
    }

    /** Lets {@code millis} pass, with every tick and finish that falls into it; a countdown
     *  started on finish runs on in the time that is left. */
    void advance(long millis) {
        while (millis > 0) {
            if (!running) {
                now += millis;
                return;
            }
            if (millisLeft <= 0) {
                running = false;
                listener.onFinish();
                continue;
            }
            long step = Math.min(millis, Math.min(TICK_MILLIS - sinceTick, millisLeft));
            now += step;
            millis -= step;
            millisLeft -= step;
            sinceTick += step;
            if (millisLeft <= 0) {
                running = false;
                listener.onFinish();
            } else if (sinceTick >= TICK_MILLIS) {
                sinceTick = 0;
                listener.onTick(millisLeft);
            }
        }
    }
}
