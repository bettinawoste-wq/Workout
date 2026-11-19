package com.wob.workout.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.wob.workout.data.entity.Reminder;
import com.wob.workout.training.Clock;
import com.wob.workout.training.ReminderSchedule;

import java.util.List;

/**
 * One inexact alarm for the next due reminder, so the app needs no exact-alarm permission.
 * It is allowed to go off while the phone dozes ({@code setAndAllowWhileIdle}), so it comes
 * at most a few minutes late; a plain inexact alarm or a window alarm would wait for the
 * next maintenance window, hours on a phone left lying. It goes to {@link ReminderReceiver},
 * which renews it.
 */
public class AndroidReminderAlarms implements ReminderAlarms {

    private final Context context;
    private final Clock clock;

    public AndroidReminderAlarms(Context context, Clock clock) {
        this.context = context.getApplicationContext();
        this.clock = clock;
    }

    @Override
    public void renew(List<Reminder> reminders) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent due = PendingIntent.getBroadcast(context, 0,
                new Intent(context, ReminderReceiver.class)
                        .setAction(ReminderReceiver.ACTION_DUE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Long dueMillis = ReminderSchedule.nextDue(reminders, clock);
        if (dueMillis == null) {
            alarmManager.cancel(due);
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueMillis, due);
        }
    }
}
