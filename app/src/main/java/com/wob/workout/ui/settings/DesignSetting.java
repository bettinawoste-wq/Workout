package com.wob.workout.ui.settings;

import android.content.SharedPreferences;

/**
 * The chosen {@link Design}, kept in preferences so it survives an app restart; a fresh
 * install follows the system.
 */
public class DesignSetting {

    private static final String KEY_DESIGN = "design";

    private final SharedPreferences preferences;

    public DesignSetting(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public Design get() {
        String stored = preferences.getString(KEY_DESIGN, null);
        for (Design design : Design.values()) {
            if (design.name().equals(stored)) return design;
        }
        return Design.SYSTEM;
    }

    public void set(Design design) {
        preferences.edit().putString(KEY_DESIGN, design.name()).apply();
    }
}
