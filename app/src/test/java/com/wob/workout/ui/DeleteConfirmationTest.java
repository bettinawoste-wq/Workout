package com.wob.workout.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DeleteConfirmationTest {

    @Test
    public void theFieldsTextComesFirst() {
        assertEquals("Arm Day", DeleteConfirmation.name("Arm Day", "Leg Day"));
    }

    @Test
    public void anEmptyFieldFallsBackToTheStoredName() {
        assertEquals("Leg Day", DeleteConfirmation.name("", "Leg Day"));
        assertEquals("Leg Day", DeleteConfirmation.name(null, "Leg Day"));
    }

    @Test
    public void withNeitherTheNameIsEmpty() {
        assertEquals("", DeleteConfirmation.name("", null));
    }
}
