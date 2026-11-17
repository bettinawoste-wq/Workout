package com.wob.workout.ui;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.wob.workout.R;

/**
 * Every screen change in the app: each screen is a Fragment in the activity's one container,
 * and back leads to the screen it was opened from.
 */
public final class Navigator {

    private final FragmentManager fragments;

    public Navigator(FragmentManager fragments) {
        this.fragments = fragments;
    }

    /** The navigator of the activity {@code screen} is shown in. */
    public static Navigator of(Fragment screen) {
        return new Navigator(screen.requireActivity().getSupportFragmentManager());
    }

    /** Shows the first screen, which back leaves the app from. */
    public void showFirst(Fragment screen) {
        fragments.beginTransaction()
                .replace(R.id.main, screen)
                .commit();
    }

    /** Opens {@code screen} on top of the current one; back returns to it. */
    public void open(Fragment screen) {
        fragments.beginTransaction()
                .replace(R.id.main, screen)
                .addToBackStack(null)
                .commit();
    }

    /** Closes the current screen and returns to the one it was opened from. */
    public void close() {
        fragments.popBackStack();
    }

    /** Puts {@code screen} in place of the current one, so back still leads where it did. */
    public void replace(Fragment screen) {
        close();
        open(screen);
    }
}
