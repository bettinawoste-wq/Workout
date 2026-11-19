package com.wob.workout.training;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A reminder's weekdays, and how they are stored: one bit each, Monday's lowest. */
public class WeekdaysTest {

    @Test
    public void storesOneBitPerDayFromMonday() {
        assertEquals(0b0000001, Weekdays.of(Weekdays.MONDAY).bits());
        assertEquals(0b1000001, Weekdays.of(Weekdays.MONDAY, Weekdays.SUNDAY).bits());
        assertEquals(0b0011111, Weekdays.MONDAY_TO_FRIDAY.bits());
    }

    @Test
    public void readsBackWhatWasStored() {
        Weekdays stored = Weekdays.fromBits(0b0010100);

        assertTrue(stored.contains(Weekdays.WEDNESDAY));
        assertTrue(stored.contains(Weekdays.FRIDAY));
        assertFalse(stored.contains(Weekdays.THURSDAY));
        assertEquals(Weekdays.of(Weekdays.WEDNESDAY, Weekdays.FRIDAY), stored);
    }

    @Test
    public void aDayCanBeSwitchedOnAndOff() {
        Weekdays days = Weekdays.NONE.with(Weekdays.TUESDAY, true);
        assertEquals(Weekdays.of(Weekdays.TUESDAY), days);
        assertTrue(days.with(Weekdays.TUESDAY, false).isEmpty());
    }

    @Test
    public void knowsNoneAndEveryDay() {
        assertTrue(Weekdays.NONE.isEmpty());
        assertFalse(Weekdays.MONDAY_TO_FRIDAY.isEveryDay());
        assertTrue(Weekdays.MONDAY_TO_FRIDAY.with(Weekdays.SATURDAY, true)
                .with(Weekdays.SUNDAY, true).isEveryDay());
    }
}
