package com.wob.workout.ui;

/**
 * What the − and + next to a number of seconds make of it: the typed value moved by one step,
 * kept within its bounds. Input that isn't a number counts as the minimum.
 */
public final class SecondsStep {

    /** For a duration with no upper bound. */
    public static final int NO_MAXIMUM = Integer.MAX_VALUE;

    private SecondsStep() {
    }

    /** @param delta one step, negative for − */
    public static int stepped(String typed, int delta, int min, int max) {
        int seconds;
        try {
            seconds = Integer.parseInt(typed.trim());
        } catch (NumberFormatException e) {
            seconds = min;
        }
        long moved = (long) Math.max(min, Math.min(max, seconds)) + delta;
        return (int) Math.max(min, Math.min(max, moved));
    }
}
