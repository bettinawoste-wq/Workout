package com.wob.workout.player;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlayerWorkoutSummaryTest {

    @Test
    public void reportsDoneAndSkippedExercises() {
        PlayerWorkoutSummary summary = new PlayerWorkoutSummary(300, 8, 10);

        assertEquals(2, summary.skippedExercises());
        assertEquals("8 von 10 erledigt, 2 übersprungen", summary.resultText());
    }

    @Test
    public void nothingSkippedReadsAsAllDone() {
        assertEquals("Alle 10 Übungen erledigt",
                new PlayerWorkoutSummary(600, 10, 10).resultText());
    }

    @Test
    public void aSingleExerciseIsNotCalledAll() {
        assertEquals("Übung erledigt", new PlayerWorkoutSummary(60, 1, 1).resultText());
    }
}
