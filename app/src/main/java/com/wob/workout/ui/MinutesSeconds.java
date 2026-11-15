package com.wob.workout.ui;

import java.util.Locale;

/** Words a duration for display as minutes and seconds, e.g. "10:27". */
public final class MinutesSeconds {

    private MinutesSeconds() {
    }

    public static String text(int seconds) {
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }
}
