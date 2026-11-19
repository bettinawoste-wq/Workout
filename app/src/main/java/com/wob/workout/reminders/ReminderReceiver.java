package com.wob.workout.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wob.workout.AppContainer;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.training.WeeklyProgress;

import java.util.TimeZone;

/**
 * Receives the reminders' alarm and the system broadcasts after which it must be set
 * again (boot, a changed time or time zone, an app update). When a reminder is due, it
 * posts the notification unless {@link WeeklyProgress#isReminderSilentToday()}; either
 * way it then sets the alarm for the next one.
 */
public class ReminderReceiver extends BroadcastReceiver {

    static final String ACTION_DUE = "com.wob.workout.action.REMINDER_DUE";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppContainer container =
                ((WorkoutApplication) context.getApplicationContext()).getAppContainer();
        boolean due = ACTION_DUE.equals(intent.getAction());
        PendingResult result = goAsync();
        container.ioExecutor.execute(() -> {
            try {
                if (due && !silentToday(container)) ReminderNotification.post(context);
                container.reminderAlarms.renew(container.reminderRepository.getAll());
            } finally {
                result.finish();
            }
        });
    }

    private static boolean silentToday(AppContainer container) {
        return WeeklyProgress.of(container.workoutHistoryRepository.getSessions(),
                        container.weeklyGoalRepository.getChanges(),
                        System.currentTimeMillis(), TimeZone.getDefault())
                .isReminderSilentToday();
    }
}
