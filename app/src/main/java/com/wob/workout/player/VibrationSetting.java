package com.wob.workout.player;

import android.content.SharedPreferences;

/**
 * Whether the phone vibrates at every moment the player plays a sound, independent of the
 * {@link SoundSetting}. One choice for every workout, kept in preferences so it survives an
 * app restart; off until it is switched on.
 */
public class VibrationSetting {

    private static final String KEY_VIBRATION_ON = "player_vibration_on";

    private final SharedPreferences preferences;

    public VibrationSetting(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public boolean isOn() {
        return preferences.getBoolean(KEY_VIBRATION_ON, false);
    }

    public void setOn(boolean on) {
        preferences.edit().putBoolean(KEY_VIBRATION_ON, on).apply();
    }
}
