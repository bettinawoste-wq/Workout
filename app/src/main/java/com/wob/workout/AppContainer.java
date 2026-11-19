package com.wob.workout;

import android.content.Context;
import android.content.SharedPreferences;

import com.wob.workout.backup.Backup;
import com.wob.workout.data.repository.ExerciseRepository;
import com.wob.workout.data.repository.ReminderRepository;
import com.wob.workout.data.repository.WeeklyGoalRepository;
import com.wob.workout.data.repository.WorkoutExerciseRepository;
import com.wob.workout.data.repository.WorkoutHistoryRepository;
import com.wob.workout.data.repository.WorkoutRepository;
import com.wob.workout.data.repository.WorkoutSummaryRepository;
import com.wob.workout.media.ExerciseThumbnails;
import com.wob.workout.media.MediaStorageManager;
import com.wob.workout.player.DefaultTransitionSetting;
import com.wob.workout.player.SoundSetting;
import com.wob.workout.player.VibrationSetting;
import com.wob.workout.reminders.AndroidReminderAlarms;
import com.wob.workout.reminders.ReminderAlarms;
import com.wob.workout.training.Clock;
import com.wob.workout.ui.settings.DesignSetting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One instance, created once in {@link WorkoutApplication#onCreate()} and held for the
 * process lifetime. Replaces scattered {@code new XRepository(context)} call sites.
 */
public class AppContainer {

    /** The app's preferences file, for the few settings that live outside the database. */
    private static final String SETTINGS_FILE = "settings";

    public final Context appContext;

    public final WorkoutRepository workoutRepository;
    public final ExerciseRepository exerciseRepository;
    public final WorkoutExerciseRepository workoutExerciseRepository;
    public final WorkoutSummaryRepository workoutSummaryRepository;
    public final WorkoutHistoryRepository workoutHistoryRepository;
    public final WeeklyGoalRepository weeklyGoalRepository;

    /** Single background thread for all Room access — see wayfinder ticket #2.
     *  <b>Must stay single-threaded:</b> {@code DatabaseSessionHistory} relies on it to run
     *  its fire-and-forget history writes in call order (issue #21). */
    public final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    /** Decodes pictures for display, apart from {@link #ioExecutor} so that a slow decode
     *  never holds up Room access. */
    public final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor();

    public final ExerciseThumbnails thumbnails;

    public final SoundSetting soundSetting;
    public final VibrationSetting vibrationSetting;
    public final DesignSetting designSetting;
    public final DefaultTransitionSetting defaultTransitionSetting;

    public final ReminderRepository reminderRepository;
    public final ReminderAlarms reminderAlarms;

    /** Now and the time zone, for the weekly goal and the reminders: the phone's own. */
    public final Clock clock = Clock.SYSTEM;

    /** Export and import of the backup, over the database and the exercises' media. */
    public final Backup backup;

    public AppContainer(Context appContext) {
        this(appContext, appContext.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE),
                new AndroidReminderAlarms(appContext, Clock.SYSTEM));
    }

    /** Test seam: tests pass a preferences file and reminder alarms of their own, so they
     *  never change the settings or the reminder alarm of the app installed on the same
     *  device. */
    public AppContainer(Context appContext, SharedPreferences settings,
            ReminderAlarms reminderAlarms) {
        this.appContext = appContext;
        this.reminderAlarms = reminderAlarms;
        soundSetting = new SoundSetting(settings);
        vibrationSetting = new VibrationSetting(settings);
        designSetting = new DesignSetting(settings);
        defaultTransitionSetting = new DefaultTransitionSetting(settings);

        thumbnails = new ExerciseThumbnails(appContext, mediaExecutor);

        workoutRepository = new WorkoutRepository(appContext);
        exerciseRepository = new ExerciseRepository(appContext);
        workoutExerciseRepository = new WorkoutExerciseRepository(appContext);
        workoutSummaryRepository = new WorkoutSummaryRepository(appContext);
        workoutHistoryRepository = new WorkoutHistoryRepository(appContext);
        weeklyGoalRepository = new WeeklyGoalRepository(appContext);
        reminderRepository = new ReminderRepository(appContext);
        backup = new Backup(appContext, MediaStorageManager.getExerciseFolder(appContext));
    }
}
