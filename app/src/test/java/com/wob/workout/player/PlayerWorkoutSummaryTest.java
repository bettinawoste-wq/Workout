package com.wob.workout.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PlayerWorkoutSummaryTest {

    @Test
    public void reportsDoneAndSkippedExercises() {
        PlayerWorkoutSummary summary = new PlayerWorkoutSummary(300, 8, 10, 0);

        assertEquals(2, summary.skippedExercises());
        assertEquals("8 von 10 erledigt, 2 übersprungen", summary.resultText());
    }

    @Test
    public void nothingSkippedReadsAsAllDone() {
        assertEquals("Alle 10 Übungen erledigt",
                new PlayerWorkoutSummary(600, 10, 10, 0).resultText());
    }

    @Test
    public void aSingleExerciseIsNotCalledAll() {
        assertEquals("Übung erledigt", new PlayerWorkoutSummary(60, 1, 1, 0).resultText());
    }

    @Test
    public void theRestTimeReadsAsMinutesAndSeconds() {
        assertEquals("Pausen: 1:30", new PlayerWorkoutSummary(600, 10, 10, 90).restText());
        assertEquals("Pausen: 0:07", PlayerWorkoutSummary.restText(7));
    }

    @Test
    public void restsEndedAtOnceShowNoTime() {
        assertEquals("Pausen: 0:00", new PlayerWorkoutSummary(600, 10, 10, 0).restText());
    }

    @Test
    public void noRestShowsNothing() {
        assertNull("no rest ran", new PlayerWorkoutSummary(600, 10, 10, null).restText());
        assertNull("recorded before rests existed", PlayerWorkoutSummary.restText(null));
    }
}
