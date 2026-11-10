package com.wob.workout.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExerciseCountTest {

    @Test
    public void oneIsSingular() {
        assertEquals("1 Übung", ExerciseCount.text(1));
    }

    @Test
    public void everythingElseIsPlural() {
        assertEquals("0 Übungen", ExerciseCount.text(0));
        assertEquals("10 Übungen", ExerciseCount.text(10));
    }
}
