package com.wob.workout.player;

/**
 * Emitted only when the screen moves to a new exercise/transition/finish — the Fragment's
 * media-display observer watches this. {@code exercise} is null during the finishing
 * transition (the 3-second countdown before {@link Phase#FINISHED}, where there is no
 * next exercise to preview); the Fragment leaves the last-shown media/title untouched in
 * that case, matching what the pre-migration Fragment code did. See wayfinder ticket #4.
 */
public class PlayerExerciseState {

    public enum Phase { READY, ACTIVE, TRANSITION, FINISHED }

    public final PlayerExercise exercise;
    public final int index;
    public final int total;
    public final Phase phase;

    public PlayerExerciseState(PlayerExercise exercise, int index, int total, Phase phase) {
        this.exercise = exercise;
        this.index = index;
        this.total = total;
        this.phase = phase;
    }
}
