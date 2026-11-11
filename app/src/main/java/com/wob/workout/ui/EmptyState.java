package com.wob.workout.ui;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wob.workout.databinding.ViewEmptyStateBinding;

/** Fills an included {@code view_empty_state} layout. Which screen shows it when is up to
 *  that screen; this only sets what it says and does. */
public final class EmptyState {

    private EmptyState() {
    }

    /** A hint only, for a screen that fills itself, like the history. */
    public static void setUp(ViewEmptyStateBinding emptyState,
                             @DrawableRes int icon, @StringRes int hint) {
        emptyState.imgEmptyIcon.setImageResource(icon);
        emptyState.txtEmptyHint.setText(hint);
        emptyState.btnEmptyAction.setVisibility(View.GONE);
    }

    /** A hint plus a button that leads to where the missing thing is created. */
    public static void setUp(ViewEmptyStateBinding emptyState,
                             @DrawableRes int icon, @StringRes int hint,
                             @StringRes int action, View.OnClickListener onAction) {
        setUp(emptyState, icon, hint);
        emptyState.btnEmptyAction.setText(action);
        emptyState.btnEmptyAction.setOnClickListener(onAction);
        emptyState.btnEmptyAction.setVisibility(View.VISIBLE);
    }

    /** Shows the empty state while {@code empty}, and hides the screen's own add control
     *  meanwhile, since the empty state's button takes over from it. */
    public static void show(ViewEmptyStateBinding emptyState, boolean empty, View addControl) {
        emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (addControl instanceof FloatingActionButton fab) {
            if (empty) fab.hide(); else fab.show();
        } else {
            addControl.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }
}
