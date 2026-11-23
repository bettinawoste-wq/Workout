package com.wob.workout.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SecondsStepTest {

    @Test
    public void plusAndMinusMoveByTheStep() {
        assertEquals(50, SecondsStep.stepped("45", 5, 1, SecondsStep.NO_MAXIMUM));
        assertEquals(40, SecondsStep.stepped(" 45 ", -5, 1, SecondsStep.NO_MAXIMUM));
    }

    @Test
    public void minusNeverGoesBelowTheMinimum() {
        assertEquals(1, SecondsStep.stepped("3", -5, 1, SecondsStep.NO_MAXIMUM));
        assertEquals(0, SecondsStep.stepped("0", -1, 0, 30));
    }

    @Test
    public void plusNeverGoesAboveTheMaximum() {
        assertEquals(30, SecondsStep.stepped("30", 1, 0, 30));
        assertEquals(30, SecondsStep.stepped("99", 1, 0, 30));
    }

    @Test
    public void unreadableInputStepsFromTheMinimum() {
        assertEquals(6, SecondsStep.stepped("", 5, 1, SecondsStep.NO_MAXIMUM));
        assertEquals(0, SecondsStep.stepped("abc", -1, 0, 30));
    }
}
