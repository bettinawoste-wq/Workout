package com.wob.workout.player;

/**
 * Emitted only when the screen moves to a new exercise/transition/finish — the Fragment's
 * media-display observer watches this. {@code exercise} is null during {@link Phase#FINISHING}
 * and {@link Phase#FINISHED} (there is no next exercise to preview); the Fragment leaves the
 * last-shown media/title untouched in that case, matching what the pre-migration Fragment
 * code did. See wayfinder ticket #4.
 */
public class PlayerExerciseState {

    /** FINISHING is the 3-second "Ende in X" countdown before FINISHED — a distinct value
     *  from TRANSITION (the between-exercise countdown) so the two aren't told apart via a
     *  null-exercise sentinel. */
    public enum Phase { READY, ACTIVE, TRANSITION, FINISHING, FINISHED }

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
