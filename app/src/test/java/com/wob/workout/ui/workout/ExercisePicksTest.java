package com.wob.workout.ui.workout;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.entity.Exercise;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExercisePicksTest {

    private final Exercise squat = exercise(1, "Kniebeugen");
    private final Exercise plank = exercise(2, "Plank");
    private final Exercise pushUp = exercise(3, "Liegestütz");

    @Test
    public void ticksStayAcrossSearchesAndAreAllAddedInTickOrder() {
        ExercisePicks picks = catalog();

        picks.search("kn");
        assertTitles(picks.shown(), "Kniebeugen");
        picks.toggle(squat.id);
        picks.search("Pl");
        picks.toggle(plank.id);

        assertTitles(picks.shown(), "Plank");
        assertTrue(picks.isPicked(squat.id));
        assertEquals(2, picks.count());
        assertTitles(picks.picked(), "Kniebeugen", "Plank");
    }

    @Test
    public void tickingAgainUnticks() {
        ExercisePicks picks = catalog();
        picks.toggle(plank.id);

        picks.toggle(plank.id);

        assertFalse(picks.isPicked(plank.id));
        assertEquals(0, picks.count());
    }

    @Test
    public void aSearchThatFindsNothingSaysSo() {
        ExercisePicks picks = catalog();

        picks.search("Burpee");

        assertTrue(picks.shown().isEmpty());
        assertTrue(picks.noMatch());
    }

    @Test
    public void restoredTicksCountOnlyForExercisesInTheCatalog() {
        ExercisePicks picks = new ExercisePicks();
        picks.restore(new long[] {3, 99, 1});
        picks.setCatalog(Arrays.asList(squat, plank, pushUp));

        assertArrayEquals(new long[] {3, 99, 1}, picks.pickedIds());
        assertTitles(picks.picked(), "Liegestütz", "Kniebeugen");
    }

    private ExercisePicks catalog() {
        ExercisePicks picks = new ExercisePicks();
        picks.setCatalog(Arrays.asList(squat, plank, pushUp));
        return picks;
    }

    private static void assertTitles(List<Exercise> exercises, String... titles) {
        List<String> actual = new ArrayList<>();
        for (Exercise exercise : exercises) actual.add(exercise.title);
        assertEquals(Arrays.asList(titles), actual);
    }

    private static Exercise exercise(long id, String title) {
        Exercise exercise = new Exercise();
        exercise.id = id;
        exercise.title = title;
        return exercise;
    }
}
