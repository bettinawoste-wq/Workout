package com.wob.workout.reminders;

import com.wob.workout.data.entity.Reminder;

import java.util.List;

/**
 * Where the reminders' next due time is handed to the system: {@link AndroidReminderAlarms}
 * in the app, a recording fake in instrumented tests, so a test never moves the alarm of
 * the app installed on the same device.
 */
public interface ReminderAlarms {

    /** Sets the one alarm for the next due reminder, or cancels it while none is on. Called
     *  after every change to the reminders, after boot and after a time or time-zone
     *  change; off the main thread. */
    void renew(List<Reminder> reminders);
}
