package com.wob.workout.training;

import com.wob.workout.data.entity.Reminder;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * When the next reminder is due. Plain Java, so the rules run in JVM tests. A reminder's
 * time is local wall-clock time, so it keeps its hour across daylight-saving changes; on
 * the day the clocks skip its hour, it is due as much later as they skip.
 */
public final class ReminderSchedule {

    private ReminderSchedule() {
    }

    /** The first moment after {@code nowMillis} at which a reminder switched on is due, or
     *  null while none is on. */
    public static Long nextDue(List<Reminder> reminders, long nowMillis, TimeZone zone) {
        Calendar now = new GregorianCalendar(zone);
        now.setTimeInMillis(nowMillis);
        // Today and the next seven days: the eighth reaches today's weekday again, for a
        // reminder whose time has already passed today.
        for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
            Long earliest = null;
            for (Reminder reminder : reminders) {
                if (!reminder.enabled) continue;
                Calendar due = new GregorianCalendar(zone);
                due.clear();
                due.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH) + dayOffset);
                if (!reminder.isOn(weekdayOf(due))) continue;
                due.set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60);
                due.set(Calendar.MINUTE, reminder.minuteOfDay % 60);
                long dueMillis = due.getTimeInMillis();
                if (dueMillis > nowMillis && (earliest == null || dueMillis < earliest)) {
                    earliest = dueMillis;
                }
            }
            if (earliest != null) return earliest;
        }
        return null;
    }

    /** 0 for Monday through 6 for Sunday. */
    private static int weekdayOf(Calendar day) {
        return (day.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }
}
