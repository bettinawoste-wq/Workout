package com.wob.workout.reminders;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.wob.workout.MainActivity;
import com.wob.workout.R;

/** The reminder's notification; tapping it opens the home screen. */
public final class ReminderNotification {

    private static final String CHANNEL_ID = "reminders";
    private static final int NOTIFICATION_ID = 1;

    private ReminderNotification() {
    }

    /** Whether the app may post notifications; the settings screen shows a hint while not. */
    public static boolean isAllowed(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Whether the notification permission still has to be asked for: from Android 13 on,
     *  until it is granted. */
    public static boolean needsPermission(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED;
    }

    // Lint can't see that needsPermission() checks the permission.
    @SuppressLint("MissingPermission")
    static void post(Context context) {
        if (needsPermission(context)) return;
        createChannel(context);
        // A fresh task, so the home screen shows whichever screen was open before.
        Intent home = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent open = PendingIntent.getActivity(context, 0, home,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID,
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_reminder)
                        .setContentTitle(context.getString(R.string.erinnerung_titel))
                        .setContentText(context.getString(R.string.erinnerung_text))
                        .setContentIntent(open)
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .build());
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        context.getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(CHANNEL_ID,
                        context.getString(R.string.erinnerungen),
                        NotificationManager.IMPORTANCE_DEFAULT));
    }
}
