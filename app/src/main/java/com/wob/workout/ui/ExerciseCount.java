package com.wob.workout.ui;

/** Words a number of exercises for display. */
public final class ExerciseCount {

    private ExerciseCount() {
    }

    public static String text(int count) {
        return count == 1 ? "1 Übung" : count + " Übungen";
    }
}
