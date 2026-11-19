package com.wob.workout;

import com.wob.workout.data.entity.Reminder;
import com.wob.workout.reminders.ReminderAlarms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Records the reminders the alarm was last renewed with, instead of setting a real alarm
 *  for the app installed on the same device. */
public class FakeReminderAlarms implements ReminderAlarms {

    private List<Reminder> renewedWith;

    @Override
    public synchronized void renew(List<Reminder> reminders) {
        renewedWith = new ArrayList<>(reminders);
    }

    /** The reminders of the last renewal, or null if the alarm was never renewed. */
    public synchronized List<Reminder> getRenewedWith() {
        return renewedWith == null ? null : Collections.unmodifiableList(renewedWith);
    }
}
