package com.wob.workout.training;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.wob.workout.data.entity.Reminder;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * When the next reminder is due, in local time. Times are written in UTC with Berlin's
 * offset worked out by hand: +1 in winter, +2 in summer (from 29 March 2026 and until
 * 25 October 2026, both Sundays).
 */
public class ReminderScheduleTest {

    private static final TimeZone BERLIN = TimeZone.getTimeZone("Europe/Berlin");

    // Weekdays, Monday first.
    private static final int MON = Reminder.MONDAY;
    private static final int TUE = MON << 1;
    private static final int WED = MON << 2;
    private static final int THU = MON << 3;
    private static final int FRI = MON << 4;
    private static final int SAT = MON << 5;
    private static final int SUN = MON << 6;
    private static final int EVERY_DAY = MON | TUE | WED | THU | FRI | SAT | SUN;

    // Thursday 19 November 2026, 10:00 in Berlin.
    private static final long THURSDAY_10_00 = utc(2026, 11, 19, 9, 0);

    @Test
    public void noReminderMeansNothingIsDue() {
        assertNull(nextDue(Collections.emptyList(), THURSDAY_10_00));
    }

    @Test
    public void remindersSwitchedOffAreNeverDue() {
        assertNull(nextDue(Arrays.asList(
                reminder(18, 0, EVERY_DAY, false),
                reminder(7, 30, THU, false)), THURSDAY_10_00));
    }

    @Test
    public void aReminderLaterTodayIsDueToday() {
        assertEquals(Long.valueOf(utc(2026, 11, 19, 17, 0)),
                nextDue(Collections.singletonList(reminder(18, 0, THU, true)),
                        THURSDAY_10_00));
    }

    @Test
    public void aTimeAlreadyPassedTodayIsDueOnItsNextWeekday() {
        assertEquals(Long.valueOf(utc(2026, 11, 26, 6, 30)),
                nextDue(Collections.singletonList(reminder(7, 30, THU, true)),
                        THURSDAY_10_00));
    }

    @Test
    public void aReminderDueRightNowIsNextDueOnItsNextWeekday() {
        assertEquals(Long.valueOf(utc(2026, 11, 20, 9, 0)),
                nextDue(Collections.singletonList(reminder(10, 0, THU | FRI, true)),
                        THURSDAY_10_00));
    }

    @Test
    public void theNextDueTimeSkipsDaysThatAreNotItsWeekdays() {
        // Monday and Wednesday: from Thursday on, the coming Monday.
        assertEquals(Long.valueOf(utc(2026, 11, 23, 17, 0)),
                nextDue(Collections.singletonList(reminder(18, 0, MON | WED, true)),
                        THURSDAY_10_00));
    }

    @Test
    public void theEarliestOfSeveralRemindersIsDue() {
        assertEquals(Long.valueOf(utc(2026, 11, 20, 6, 0)),
                nextDue(Arrays.asList(
                        reminder(18, 0, SAT, true),
                        reminder(7, 0, FRI, true),
                        reminder(6, 0, FRI, false),
                        reminder(20, 0, FRI, true)), THURSDAY_10_00));
    }

    @Test
    public void aReminderKeepsItsLocalTimeWhenDaylightSavingStarts() {
        // Saturday 28 March, 9:00 in winter time; Sunday's 8:00 is summer time.
        assertEquals(Long.valueOf(utc(2026, 3, 29, 6, 0)),
                nextDue(Collections.singletonList(reminder(8, 0, EVERY_DAY, true)),
                        utc(2026, 3, 28, 8, 0)));
    }

    @Test
    public void aReminderKeepsItsLocalTimeWhenDaylightSavingEnds() {
        // Saturday 24 October, 9:00 in summer time; Sunday's 8:00 is winter time.
        assertEquals(Long.valueOf(utc(2026, 10, 25, 7, 0)),
                nextDue(Collections.singletonList(reminder(8, 0, EVERY_DAY, true)),
                        utc(2026, 10, 24, 7, 0)));
    }

    @Test
    public void aReminderInTheHourSkippedByDaylightSavingIsDueAnHourLater() {
        // 2:30 doesn't exist on 29 March: the clocks go from 2:00 straight to 3:00.
        assertEquals(Long.valueOf(utc(2026, 3, 29, 1, 30)),
                nextDue(Collections.singletonList(reminder(2, 30, SUN, true)),
                        utc(2026, 3, 28, 8, 0)));
    }

    private static Long nextDue(List<Reminder> reminders, long nowMillis) {
        return ReminderSchedule.nextDue(reminders, nowMillis, BERLIN);
    }

    private static Reminder reminder(int hour, int minute, int weekdays, boolean enabled) {
        return new Reminder(hour * 60 + minute, weekdays, enabled);
    }

    private static long utc(int year, int month, int day, int hour, int minute) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute);
        return calendar.getTimeInMillis();
    }
}
