package com.wob.workout.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinutesSecondsTest {

    @Test
    public void secondsAlwaysHaveTwoDigits() {
        assertEquals("0:00", MinutesSeconds.text(0));
        assertEquals("1:05", MinutesSeconds.text(65));
        assertEquals("10:27", MinutesSeconds.text(627));
    }

    @Test
    public void minutesAreNotCappedAtAnHour() {
        assertEquals("75:00", MinutesSeconds.text(4500));
    }
}
