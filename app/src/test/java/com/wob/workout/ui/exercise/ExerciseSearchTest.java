package com.wob.workout.ui.exercise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.entity.Exercise;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExerciseSearchTest {

    private static final List<Exercise> CATALOG = Arrays.asList(
            exercise("Kniebeugen", "Beine"), exercise("Plank", "Knie am Boden"),
            exercise("Liegestütz", "Arme"));

    @Test
    public void anEmptySearchShowsEverything() {
        assertEquals(CATALOG, ExerciseSearch.matching(CATALOG, ""));
        assertEquals(CATALOG, ExerciseSearch.matching(CATALOG, "  "));
    }

    @Test
    public void aTitleMatchesWhenItContainsTheTextIgnoringCase() {
        assertTitles(ExerciseSearch.matching(CATALOG, "kn"), "Kniebeugen");
        assertTitles(ExerciseSearch.matching(CATALOG, "STÜTZ"), "Liegestütz");
    }

    @Test
    public void onlyTheTitleIsSearched() {
        assertTitles(ExerciseSearch.matching(CATALOG, "Boden"));
    }

    @Test
    public void thereIsNoMatchOnlyWhenASearchFindsNothing() {
        assertTrue(ExerciseSearch.noMatch(CATALOG, "Burpee"));
        assertFalse(ExerciseSearch.noMatch(CATALOG, "Plank"));
        assertFalse(ExerciseSearch.noMatch(CATALOG, ""));
        // An empty catalog has its own empty state, not "Keine Übung gefunden".
        assertFalse(ExerciseSearch.noMatch(new ArrayList<>(), "Burpee"));
    }

    private static void assertTitles(List<Exercise> exercises, String... titles) {
        List<String> actual = new ArrayList<>();
        for (Exercise exercise : exercises) actual.add(exercise.title);
        assertEquals(Arrays.asList(titles), actual);
    }

    private static Exercise exercise(String title, String description) {
        Exercise exercise = new Exercise();
        exercise.title = title;
        exercise.description = description;
        return exercise;
    }
}
