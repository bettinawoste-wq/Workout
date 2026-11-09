package com.wob.workout.player;

/**
 * How much of a whole workout is still to come: its exercises and the transitions between
 * them. The start countdown and the closing countdown don't count.
 */
public class PlayerProgress {

    private final int[] exerciseSeconds;
    private final long transitionMillis;
    private final long totalMillis;

    public PlayerProgress(int[] exerciseSeconds, long transitionMillis) {
        this.exerciseSeconds = exerciseSeconds;
        this.transitionMillis = transitionMillis;
        this.totalMillis = millisAfter(-1);
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
                return millisLeft + millisAfter(index);
            case TRANSITION:
                // index is already the exercise this transition leads to.
                return millisLeft + exerciseSeconds[index] * 1000L + millisAfter(index);
            default:
                return 0;
        }
    }

    /** Everything following the exercise at {@code index}, with the transitions before each. */
    private long millisAfter(int index) {
        long millis = 0;
        for (int i = index + 1; i < exerciseSeconds.length; i++) {
            millis += exerciseSeconds[i] * 1000L;
            if (i > 0) millis += transitionMillis;
        }
        return millis;
    }
}
