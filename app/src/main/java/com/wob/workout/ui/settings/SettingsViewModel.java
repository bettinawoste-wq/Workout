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
import com.wob.workout.training.Clock;
import com.wob.workout.training.Weekdays;
import com.wob.workout.ui.WholeNumber;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * The settings screen: the app-wide choices, read from preferences when the screen opens
 * and stored on every change. The sound switch is the player's own {@link SoundSetting}, so
 * switching it here or in the player's toolbar switches both; only one screen is visible at
 * a time, so reading it when the screen opens keeps them in step. The weekly goal lives in
 * the database with its history of changes, so it loads and stores off the main thread,
 * and so do the reminders, which renew the reminder alarm after every change. The input
 * rules live here: a goal of 0 training days is none, a new reminder's time and weekdays,
 * and that a reminder needs at least one weekday.
 */
public class SettingsViewModel extends ViewModel {

    /** The weekly goal's bounds in training days; no goal at all is "Kein Ziel". */
    public static final int MIN_GOAL_DAYS = 1;
    public static final int MAX_GOAL_DAYS = 7;

    /** A new reminder's time and weekdays until changed: 18:00, Monday to Friday. */
    private static final int NEW_REMINDER_MINUTE_OF_DAY = 18 * 60;
    private static final Weekdays NEW_REMINDER_WEEKDAYS = Weekdays.MONDAY_TO_FRIDAY;

    private final DesignSetting designSetting;
    private final SoundSetting soundSetting;
    private final VibrationSetting vibrationSetting;
    private final DefaultTransitionSetting defaultTransitionSetting;
    private final WeeklyGoalRepository weeklyGoalRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderAlarms reminderAlarms;
    private final ExecutorService ioExecutor;
    private final Clock clock;

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
        clock = container.clock;
        LiveDataExecutor.runOnExecutorOnce(ioExecutor,
                () -> weeklyGoalRepository.getGoal(clock), weeklyGoal::setValue);
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

    /** Takes the default transition as typed into its pop-up; every workout without its own
     *  length follows it.
     *  @return false, changing nothing, unless it is a whole number of seconds within
     *  {@link DefaultTransitionSetting#MIN_SECONDS} to
     *  {@link DefaultTransitionSetting#MAX_SECONDS} */
    public boolean changeDefaultTransition(String typed) {
        Integer seconds = WholeNumber.within(typed, DefaultTransitionSetting.MIN_SECONDS,
                DefaultTransitionSetting.MAX_SECONDS);
        if (seconds == null) return false;
        setDefaultTransitionSeconds(seconds);
        return true;
    }

    /** "Auf 3 s zurücksetzen": back to what a fresh install starts with. */
    public void resetDefaultTransition() {
        setDefaultTransitionSeconds(DefaultTransitionSetting.FRESH_INSTALL_SECONDS);
    }

    private void setDefaultTransitionSeconds(int seconds) {
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

    /** Takes the weekly goal as typed into its pop-up, from the current week on.
     *  @return false, changing nothing, unless it is {@link #MIN_GOAL_DAYS} to
     *  {@link #MAX_GOAL_DAYS} training days */
    public boolean changeWeeklyGoal(String typed) {
        Integer trainingDays = WholeNumber.within(typed, MIN_GOAL_DAYS, MAX_GOAL_DAYS);
        if (trainingDays == null) return false;
        setWeeklyGoal(trainingDays);
        return true;
    }

    /** "Kein Ziel": removes the weekly goal from the current week on. */
    public void removeWeeklyGoal() {
        setWeeklyGoal(null);
    }

    private void setWeeklyGoal(Integer goal) {
        weeklyGoal.setValue(goal);
        LiveDataExecutor.runOnExecutorOnce(ioExecutor, () -> {
            weeklyGoalRepository.setGoal(goal, clock);
            return null;
        }, result -> { });
    }

    /** The reminders, ordered by time of day, kept live. */
    public LiveData<List<Reminder>> getReminders() {
        return reminderRepository.observeAll();
    }

    /** The time of day, in minutes, a new reminder starts with. */
    public int newReminderMinuteOfDay() {
        return NEW_REMINDER_MINUTE_OF_DAY;
    }

    /** The weekdays a new reminder starts with. */
    public Weekdays newReminderWeekdays() {
        return NEW_REMINDER_WEEKDAYS;
    }

    /** Whether a reminder on these weekdays can be saved: it needs at least one. */
    public boolean canSaveReminder(Weekdays weekdays) {
        return !weekdays.isEmpty();
    }

    /** Adds a reminder, switched on; one that can't be saved is not stored. */
    public void addReminder(int minuteOfDay, Weekdays weekdays) {
        if (!canSaveReminder(weekdays)) return;
        Reminder reminder = new Reminder(minuteOfDay, weekdays, true);
        changeReminders(() -> reminderRepository.insert(reminder));
    }

    /** Changes a reminder's time and weekdays; if it can't be saved, it stays as it was. */
    public void changeReminder(Reminder reminder, int minuteOfDay, Weekdays weekdays) {
        if (!canSaveReminder(weekdays)) return;
        Reminder changed = copyOf(reminder);
        changed.minuteOfDay = minuteOfDay;
        changed.weekdays = weekdays.bits();
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
        Reminder copy = new Reminder(reminder.minuteOfDay, reminder.days(), reminder.enabled);
        copy.id = reminder.id;
        return copy;
    }
}
