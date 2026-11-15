package com.wob.workout.player;

import android.content.SharedPreferences;

/**
 * Whether the player makes its sounds (countdown beeps, start sound, success sound). One
 * choice for every workout, kept in preferences so it survives an app restart; on until it is
 * switched off.
 */
public class SoundSetting {

    private static final String KEY_SOUND_ON = "player_sound_on";

    private final SharedPreferences preferences;

    public SoundSetting(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public boolean isOn() {
        return preferences.getBoolean(KEY_SOUND_ON, true);
    }

    public void setOn(boolean on) {
        preferences.edit().putBoolean(KEY_SOUND_ON, on).apply();
    }
}
