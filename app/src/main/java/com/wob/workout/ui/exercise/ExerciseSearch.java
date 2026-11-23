package com.wob.workout.ui.exercise;

import com.wob.workout.data.entity.Exercise;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The search above the exercise list and the exercise picker: an exercise matches when its
 * title contains the typed text, ignoring case. Only the title is searched.
 */
public final class ExerciseSearch {

    private ExerciseSearch() {
    }

    /** The exercises whose title matches, in their order; all of them for an empty search. */
    public static List<Exercise> matching(List<Exercise> exercises, String typed) {
        String wanted = normalized(typed);
        if (wanted.isEmpty()) return exercises;
        List<Exercise> matches = new ArrayList<>();
        for (Exercise exercise : exercises) {
            if (exercise.title != null && normalized(exercise.title).contains(wanted)) {
                matches.add(exercise);
            }
        }
        return matches;
    }

    /** Whether to say "Keine Übung gefunden": a search found nothing in a catalog that has
     *  exercises (an empty catalog has its own empty state). */
    public static boolean noMatch(List<Exercise> exercises, String typed) {
        return !exercises.isEmpty() && matching(exercises, typed).isEmpty();
    }

    private static String normalized(String text) {
        return text.trim().toLowerCase(Locale.GERMAN);
    }
}
