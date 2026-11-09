package com.wob.workout.player;

/**
 * What the completion screen reports about a finished workout session. An exercise counts as
 * done if its time ran out at least once; stepping back to a skipped one and finishing it counts.
 */
public class PlayerWorkoutSummary {

    public final int activeSeconds;
    public final int doneExercises;
    public final int totalExercises;

    public PlayerWorkoutSummary(int activeSeconds, int doneExercises, int totalExercises) {
        this.activeSeconds = activeSeconds;
        this.doneExercises = doneExercises;
        this.totalExercises = totalExercises;
    }

    public int skippedExercises() {
        return totalExercises - doneExercises;
    }

    public String resultText() {
        if (skippedExercises() == 0) {
            return totalExercises == 1
                    ? "Übung erledigt"
                    : "Alle " + totalExercises + " Übungen erledigt";
        }
        return doneExercises + " von " + totalExercises + " erledigt, "
                + skippedExercises() + " übersprungen";
    }
}
