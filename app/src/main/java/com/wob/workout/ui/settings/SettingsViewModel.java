package com.wob.workout.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.repository.WeeklyGoalRepository;
import com.wob.workout.player.DefaultTransitionSetting;
import com.wob.workout.player.SoundSetting;
import com.wob.workout.player.VibrationSetting;

import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

/**
 * The settings screen: the app-wide choices, read from preferences when the screen opens
 * and stored on every change. The sound switch is the player's own {@link SoundSetting}, so
 * switching it here or in the player's toolbar switches both; only one screen is visible at
 * a time, so reading it when the screen opens keeps them in step. The weekly goal lives in
 * the database with its history of changes, so it loads and stores off the main thread.
 */
public class SettingsViewModel extends ViewModel {

    private final DesignSetting designSetting;
    private final SoundSetting soundSetting;
    private final VibrationSetting vibrationSetting;
    private final DefaultTransitionSetting defaultTransitionSetting;
    private final WeeklyGoalRepository weeklyGoalRepository;
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
}
