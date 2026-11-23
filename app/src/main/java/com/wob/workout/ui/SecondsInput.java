package com.wob.workout.ui;

import android.view.View;
import android.widget.EditText;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.wob.workout.R;

/**
 * Wires a number-of-seconds field to the − and + beside it ({@code btnLess}, {@code btnMore}
 * in the dialog's layout) and, optionally, to quick-pick chips. What a step makes of the typed
 * value is {@link SecondsStep}'s rule; the field stays the one place the value is read from.
 */
public final class SecondsInput {

    private SecondsInput() {
    }

    /** @param dialogView the dialog's view holding {@code btnLess} and {@code btnMore} */
    public static void attach(View dialogView, EditText field, int step, int min, int max) {
        dialogView.findViewById(R.id.btnLess).setOnClickListener(v -> move(field, -step, min, max));
        dialogView.findViewById(R.id.btnMore).setOnClickListener(v -> move(field, step, min, max));
    }

    /** Adds one chip per value to {@code chips}; tapping one puts its value in the field. */
    public static void addQuickPicks(ChipGroup chips, EditText field, int... seconds) {
        for (int value : seconds) {
            Chip chip = new Chip(chips.getContext());
            chip.setText(chips.getContext().getString(R.string.sekunden_kurz, value));
            chip.setOnClickListener(v -> show(field, value));
            chips.addView(chip);
        }
    }

    private static void move(EditText field, int delta, int min, int max) {
        String typed = field.getText() != null ? field.getText().toString() : "";
        show(field, SecondsStep.stepped(typed, delta, min, max));
    }

    private static void show(EditText field, int seconds) {
        field.setText(String.valueOf(seconds));
        field.setSelection(field.length());
    }
}
