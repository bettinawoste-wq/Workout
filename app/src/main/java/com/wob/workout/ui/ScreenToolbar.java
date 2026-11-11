package com.wob.workout.ui;

import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.wob.workout.R;

/** The title bar every screen except home has: a back arrow and the screen's name. */
public final class ScreenToolbar {

    private ScreenToolbar() {
    }

    /** The arrow goes through the activity's back dispatcher rather than popping directly,
     *  so it always leads where system back does, including a screen's own back handling
     *  (e.g. the player asking before it aborts a workout). */
    public static void setUp(Fragment fragment, MaterialToolbar toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationContentDescription(R.string.zurueck);
        toolbar.setNavigationOnClickListener(v ->
                fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }
}
