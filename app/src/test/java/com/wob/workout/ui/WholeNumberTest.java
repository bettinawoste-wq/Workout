package com.wob.workout.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WholeNumberTest {

    @Test
    public void aTypedWholeNumberWithinItsBoundsIsTaken() {
        assertEquals(Integer.valueOf(5), WholeNumber.within(" 5 ", 1, 7));
        assertEquals(Integer.valueOf(1), WholeNumber.within("1", 1, 7));
        assertEquals(Integer.valueOf(7), WholeNumber.within("7", 1, 7));
    }

    @Test
    public void anythingElseIsNot() {
        assertNull(WholeNumber.within("0", 1, 7));
        assertNull(WholeNumber.within("8", 1, 7));
        assertNull(WholeNumber.within("", 1, 7));
        assertNull(WholeNumber.within("2,5", 1, 7));
    }
}
