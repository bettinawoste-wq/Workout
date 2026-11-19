package com.wob.workout.player;

import com.wob.workout.ui.MinutesSeconds;

/**
 * What the completion screen reports about a finished workout session. An exercise counts as
 * done if its time ran out at least once; stepping back to a skipped one and finishing it counts.
 * Rests are not exercises: their time is reported on its own.
 */
public class PlayerWorkoutSummary {

    public final int activeSeconds;
    public final int doneExercises;
    public final int totalExercises;
    /** How long the workout session rested, pauses left out. */
    public final int restSeconds;

    public PlayerWorkoutSummary(int activeSeconds, int doneExercises, int totalExercises,
                                int restSeconds) {
        this.activeSeconds = activeSeconds;
        this.doneExercises = doneExercises;
        this.totalExercises = totalExercises;
        this.restSeconds = restSeconds;
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

    /** E.g. "Pausen: 1:30"; null for a workout session without rest time. */
    public String restText() {
        return restText(restSeconds);
    }

    /** E.g. "Pausen: 1:30"; null when nothing was rested, or for a workout session recorded
     *  before rests existed (null). */
    public static String restText(Integer restSeconds) {
        if (restSeconds == null || restSeconds <= 0) return null;
        return "Pausen: " + MinutesSeconds.text(restSeconds);
    }
}
