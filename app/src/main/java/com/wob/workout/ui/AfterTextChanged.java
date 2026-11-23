package com.wob.workout.ui;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

/** A {@link TextWatcher} for when only the text after a change matters, e.g. a search. */
public final class AfterTextChanged implements TextWatcher {

    private final Consumer<String> then;

    public AfterTextChanged(Consumer<String> then) {
        this.then = then;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        then.accept(s.toString());
    }
}
