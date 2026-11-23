package com.wob.workout.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NumberStepTest {

    @Test
    public void plusAndMinusMoveByTheStep() {
        assertEquals(50, NumberStep.stepped("45", 5, 1, NumberStep.NO_MAXIMUM));
        assertEquals(40, NumberStep.stepped(" 45 ", -5, 1, NumberStep.NO_MAXIMUM));
    }

    @Test
    public void minusNeverGoesBelowTheMinimum() {
        assertEquals(1, NumberStep.stepped("3", -5, 1, NumberStep.NO_MAXIMUM));
        assertEquals(0, NumberStep.stepped("0", -1, 0, 30));
    }

    @Test
    public void plusNeverGoesAboveTheMaximum() {
        assertEquals(30, NumberStep.stepped("30", 1, 0, 30));
        assertEquals(30, NumberStep.stepped("99", 1, 0, 30));
    }

    @Test
    public void unreadableInputStepsFromTheMinimum() {
        assertEquals(6, NumberStep.stepped("", 5, 1, NumberStep.NO_MAXIMUM));
        assertEquals(0, NumberStep.stepped("abc", -1, 0, 30));
    }
}
