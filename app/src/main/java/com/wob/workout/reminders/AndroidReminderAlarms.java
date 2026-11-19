package com.wob.workout.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.wob.workout.data.entity.Reminder;
import com.wob.workout.training.ReminderSchedule;

import java.util.List;
import java.util.TimeZone;

/**
 * One inexact alarm for the next due reminder, so the app needs no exact-alarm permission:
 * a window of ten minutes from its time (the shortest the system grants), within which the
 * system may deliver it. A plain inexact alarm would get a window of up to three quarters
 * of the time until it is due, hours for an evening reminder set in the morning. It goes
 * to {@link ReminderReceiver}, which renews it.
 */
public class AndroidReminderAlarms implements ReminderAlarms {

    private static final long WINDOW_MILLIS = 10 * 60 * 1000;

    private final Context context;

    public AndroidReminderAlarms(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void renew(List<Reminder> reminders) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent due = PendingIntent.getBroadcast(context, 0,
                new Intent(context, ReminderReceiver.class)
                        .setAction(ReminderReceiver.ACTION_DUE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Long dueMillis = ReminderSchedule.nextDue(
                reminders, System.currentTimeMillis(), TimeZone.getDefault());
        if (dueMillis == null) {
            alarmManager.cancel(due);
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, dueMillis, WINDOW_MILLIS, due);
        }
    }
}
