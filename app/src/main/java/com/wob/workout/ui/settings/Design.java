package com.wob.workout.ui.settings;

import androidx.appcompat.app.AppCompatDelegate;

/** How the app looks: following the phone's light or dark mode, or always one of them. */
public enum Design {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    private final int nightMode;

    Design(int nightMode) {
        this.nightMode = nightMode;
    }

    /** Puts this design in place at once, on every screen, through the platform's
     *  night-mode switch; open screens are recreated in it. */
    public void apply() {
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
