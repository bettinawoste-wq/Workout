package com.wob.workout.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.player.SoundSetting;
import com.wob.workout.player.VibrationSetting;

/**
 * The settings screen: the app-wide choices, read from preferences when the screen opens
 * and stored on every change. The sound switch is the player's own {@link SoundSetting}, so
 * switching it here or in the player's toolbar switches both; only one screen is visible at
 * a time, so reading it when the screen opens keeps them in step.
 */
public class SettingsViewModel extends ViewModel {

    private final DesignSetting designSetting;
    private final SoundSetting soundSetting;
    private final VibrationSetting vibrationSetting;

    private final MutableLiveData<Design> design = new MutableLiveData<>();
    private final MutableLiveData<Boolean> soundOn = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibrationOn = new MutableLiveData<>();

    public SettingsViewModel(AppContainer container) {
        designSetting = container.designSetting;
        soundSetting = container.soundSetting;
        vibrationSetting = container.vibrationSetting;
        design.setValue(designSetting.get());
        soundOn.setValue(soundSetting.isOn());
        vibrationOn.setValue(vibrationSetting.isOn());
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
}
