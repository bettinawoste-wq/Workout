package com.wob.workout.player;

import android.content.SharedPreferences;

/**
 * The default transition: how long the transition lasts in every workout that sets no
 * length of its own. Kept in preferences so it survives an app restart; 3 seconds until it
 * is changed.
 */
public class DefaultTransitionSetting {

    public static final int MIN_SECONDS = 0;
    public static final int MAX_SECONDS = 30;
    public static final int FRESH_INSTALL_SECONDS = 3;

    private static final String KEY_SECONDS = "player_default_transition_seconds";

    private final SharedPreferences preferences;

    public DefaultTransitionSetting(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public int getSeconds() {
        return preferences.getInt(KEY_SECONDS, FRESH_INSTALL_SECONDS);
    }

    /** Clamps to {@link #MIN_SECONDS} to {@link #MAX_SECONDS}. */
    public void setSeconds(int seconds) {
        int clamped = Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
        preferences.edit().putInt(KEY_SECONDS, clamped).apply();
    }

    /** What a workout's transition lasts: its own length, otherwise this default. */
    public int resolve(Integer workoutSeconds) {
        return workoutSeconds != null ? workoutSeconds : getSeconds();
    }
}
