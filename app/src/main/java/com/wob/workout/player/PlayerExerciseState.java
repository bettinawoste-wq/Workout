package com.wob.workout.player;

/**
 * Emitted only when the screen moves to a new exercise/transition/finish — the Fragment's
 * media-display observer watches this. {@code exercise} is null during {@link Phase#FINISHED}.
 * See wayfinder ticket #4.
 */
public class PlayerExerciseState {

    /** FINISHED follows the last exercise directly: its own last seconds are the countdown. */
    public enum Phase { READY, ACTIVE, TRANSITION, FINISHED }

    public final PlayerExercise exercise;
    public final int index;
    public final int total;
    public final Phase phase;
    /** Only set during {@link Phase#FINISHED}. */
    public final PlayerWorkoutSummary summary;

    public PlayerExerciseState(PlayerExercise exercise, int index, int total, Phase phase) {
        this(exercise, index, total, phase, null);
    }

    public PlayerExerciseState(PlayerExercise exercise, int index, int total, Phase phase,
                               PlayerWorkoutSummary summary) {
        this.exercise = exercise;
        this.index = index;
        this.total = total;
        this.phase = phase;
        this.summary = summary;
    }

    /** False on the last exercise, where {@code PlayerViewModel.next()} does nothing. */
    public boolean hasNext() {
        return index < total - 1;
    }

    /** False on the first exercise, where {@code PlayerViewModel.previous()} does nothing. */
    public boolean hasPrevious() {
        return index > 0;
    }
}
