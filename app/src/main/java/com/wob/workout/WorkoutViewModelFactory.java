package com.wob.workout;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * One generic factory for the whole app: add one branch per ViewModel as screens
 * migrate, rather than a bespoke Factory class for each one. See wayfinder ticket #2.
 */
public class WorkoutViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;

    public WorkoutViewModelFactory(AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }
}
