package com.wob.workout.player;

/**
 * How much exercise time of a whole workout is still to come. Pauses don't count: during the
 * start countdown and between exercises the remaining time stands still.
 */
public class PlayerProgress {

    private final int[] exerciseSeconds;
    private final long totalMillis;

    public PlayerProgress(int[] exerciseSeconds) {
        this.exerciseSeconds = exerciseSeconds;
        this.totalMillis = millisFrom(0);
    }

    public long totalMillis() {
        return totalMillis;
    }

    /** @param millisLeft what is left of the current phase's own countdown */
    public long remainingMillis(int index, PlayerExerciseState.Phase phase, long millisLeft) {
        switch (phase) {
            case READY:
                return totalMillis;
            case ACTIVE:
                return millisLeft + millisFrom(index + 1);
            case TRANSITION:
                // index is already the exercise this pause leads to.
                return millisFrom(index);
            default:
                return 0;
        }
    }

    /** The exercise time from the exercise at {@code index} to the end. */
    private long millisFrom(int index) {
        long millis = 0;
        for (int i = index; i < exerciseSeconds.length; i++) {
            millis += exerciseSeconds[i] * 1000L;
        }
        return millis;
    }
}
