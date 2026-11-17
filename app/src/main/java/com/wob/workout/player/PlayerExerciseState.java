package com.wob.workout.player;

import com.wob.workout.data.WorkoutExercise;

/**
 * Emitted only when the screen moves to a new exercise/transition/finish — the Fragment's
 * media-display observer watches this. {@code exercise} is null during {@link Phase#FINISHED}.
 * See wayfinder ticket #4.
 */
public class PlayerExerciseState {

    /** FINISHED follows the last exercise directly: its own last seconds are the countdown. */
    public enum Phase { READY, ACTIVE, TRANSITION, FINISHED }

    public final WorkoutExercise exercise;
    public final int index;
    public final int total;
    public final Phase phase;
    /** Only set during {@link Phase#FINISHED}. */
    public final PlayerWorkoutSummary summary;
    /** An exercise a countdown led into starts with the start sound; one stepped to doesn't. */
    public final boolean startSound;
    /** A completed workout ends with the success sound. */
    public final boolean successSound;

    public PlayerExerciseState(WorkoutExercise exercise, int index, int total, Phase phase) {
        this(exercise, index, total, phase, false);
    }

    public PlayerExerciseState(WorkoutExercise exercise, int index, int total, Phase phase,
                               boolean startSound) {
        this(exercise, index, total, phase, null, startSound, false);
    }

    /** The finished workout: no exercise, its summary, the success sound. */
    public static PlayerExerciseState finished(int index, int total,
                                               PlayerWorkoutSummary summary) {
        return new PlayerExerciseState(null, index, total, Phase.FINISHED, summary, false, true);
    }

    private PlayerExerciseState(WorkoutExercise exercise, int index, int total, Phase phase,
                                PlayerWorkoutSummary summary, boolean startSound,
                                boolean successSound) {
        this.exercise = exercise;
        this.index = index;
        this.total = total;
        this.phase = phase;
        this.summary = summary;
        this.startSound = startSound;
        this.successSound = successSound;
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
