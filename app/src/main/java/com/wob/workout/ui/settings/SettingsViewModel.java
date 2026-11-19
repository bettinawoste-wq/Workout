package com.wob.workout.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.Reminder;
import com.wob.workout.data.repository.ReminderRepository;
import com.wob.workout.data.repository.WeeklyGoalRepository;
import com.wob.workout.player.DefaultTransitionSetting;
import com.wob.workout.player.SoundSetting;
import com.wob.workout.player.VibrationSetting;
import com.wob.workout.reminders.ReminderAlarms;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

/**
 * The settings screen: the app-wide choices, read from preferences when the screen opens
 * and stored on every change. The sound switch is the player's own {@link SoundSetting}, so
 * switching it here or in the player's toolbar switches both; only one screen is visible at
 * a time, so reading it when the screen opens keeps them in step. The weekly goal lives in
 * the database with its history of changes, so it loads and stores off the main thread,
 * and so do the reminders, which renew the reminder alarm after every change.
 */
public class SettingsViewModel extends ViewModel {

    private final DesignSetting designSetting;
    private final SoundSetting soundSetting;
    private final VibrationSetting vibrationSetting;
    private final DefaultTransitionSetting defaultTransitionSetting;
    private final WeeklyGoalRepository weeklyGoalRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderAlarms reminderAlarms;
    private final ExecutorService ioExecutor;

    private final MutableLiveData<Design> design = new MutableLiveData<>();
    private final MutableLiveData<Boolean> soundOn = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibrationOn = new MutableLiveData<>();
    private final MutableLiveData<Integer> defaultTransitionSeconds = new MutableLiveData<>();
    private final MutableLiveData<Integer> weeklyGoal = new MutableLiveData<>();

    public SettingsViewModel(AppContainer container) {
        designSetting = container.designSetting;
        soundSetting = container.soundSetting;
        vibrationSetting = container.vibrationSetting;
        defaultTransitionSetting = container.defaultTransitionSetting;
        design.setValue(designSetting.get());
        soundOn.setValue(soundSetting.isOn());
        vibrationOn.setValue(vibrationSetting.isOn());
        defaultTransitionSeconds.setValue(defaultTransitionSetting.getSeconds());
        weeklyGoalRepository = container.weeklyGoalRepository;
        reminderRepository = container.reminderRepository;
        reminderAlarms = container.reminderAlarms;
        ioExecutor = container.ioExecutor;
        LiveDataExecutor.runOnExecutorOnce(ioExecutor,
                () -> weeklyGoalRepository.getGoal(
                        System.currentTimeMillis(), TimeZone.getDefault()),
                weeklyGoal::setValue);
    }

    public LiveData<Design> getDesign() {
        return design;
    }

    public LiveData<Boolean> isSoundOn() {
        return soundOn;
    }

    public LiveData<Boolean> isVibrationOn() {
        return vibrationOn;
    }

    /** The default transition's length in seconds. */
    public LiveData<Integer> getDefaultTransitionSeconds() {
        return defaultTransitionSeconds;
    }

    /** Stores the default transition; every workout without its own length follows it. */
    public void setDefaultTransitionSeconds(int seconds) {
        defaultTransitionSetting.setSeconds(seconds);
        defaultTransitionSeconds.setValue(defaultTransitionSetting.getSeconds());
    }

    /** Stores the design; the screen puts it in place with {@link Design#apply()}. */
    public void setDesign(Design chosen) {
        designSetting.set(chosen);
        design.setValue(chosen);
    }

    public void setSoundOn(boolean on) {
        soundSetting.setOn(on);
        soundOn.setValue(on);
    }

    public void setVibrationOn(boolean on) {
        vibrationSetting.setOn(on);
        vibrationOn.setValue(on);
    }

    /** The weekly goal in training days, or null without one; set once it has loaded. */
    public LiveData<Integer> getWeeklyGoal() {
        return weeklyGoal;
    }

    /** Sets the weekly goal from the current week on, or removes it with null. */
    public void setWeeklyGoal(Integer trainingDays) {
        weeklyGoal.setValue(trainingDays);
        long now = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        LiveDataExecutor.runOnExecutorOnce(ioExecutor, () -> {
            weeklyGoalRepository.setGoal(trainingDays, now, zone);
            return null;
        }, result -> { });
    }

    /** The reminders, ordered by time of day, kept live. */
    public LiveData<List<Reminder>> getReminders() {
        return reminderRepository.observeAll();
    }

    /** Adds a reminder, switched on; one without weekdays is not stored. */
    public void addReminder(int minuteOfDay, int weekdays) {
        if (weekdays == 0) return;
        Reminder reminder = new Reminder(minuteOfDay, weekdays, true);
        changeReminders(() -> reminderRepository.insert(reminder));
    }

    /** Changes a reminder's time and weekdays; without weekdays it stays as it was. */
    public void changeReminder(Reminder reminder, int minuteOfDay, int weekdays) {
        if (weekdays == 0) return;
        Reminder changed = copyOf(reminder);
        changed.minuteOfDay = minuteOfDay;
        changed.weekdays = weekdays;
        changeReminders(() -> reminderRepository.update(changed));
    }

    public void setReminderOn(Reminder reminder, boolean on) {
        Reminder changed = copyOf(reminder);
        changed.enabled = on;
        changeReminders(() -> reminderRepository.update(changed));
    }

    public void deleteReminder(Reminder reminder) {
        changeReminders(() -> reminderRepository.delete(reminder));
    }

    private void changeReminders(Runnable change) {
        ioExecutor.execute(() -> {
            change.run();
            reminderAlarms.renew(reminderRepository.getAll());
        });
    }

    private static Reminder copyOf(Reminder reminder) {
        Reminder copy = new Reminder(reminder.minuteOfDay, reminder.weekdays, reminder.enabled);
        copy.id = reminder.id;
        return copy;
    }
}
