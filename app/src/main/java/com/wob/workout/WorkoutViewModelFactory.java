package com.wob.workout;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.wob.workout.player.PlayerViewModel;
import com.wob.workout.ui.workout.WorkoutListViewModel;

/**
 * One generic factory for the whole app: add one branch per ViewModel as screens
 * migrate, rather than a bespoke Factory class for each one. See wayfinder ticket #2.
 *
 * <p>Screens whose ViewModel needs a navigation argument (e.g. {@code workoutId}) pass it
 * to the constructor here and it's threaded through in the matching branch below.
 */
public class WorkoutViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;
    private final long workoutId;

    public WorkoutViewModelFactory(AppContainer container) {
        this(container, -1);
    }

    public WorkoutViewModelFactory(AppContainer container, long workoutId) {
        this.container = container;
        this.workoutId = workoutId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WorkoutListViewModel.class)) {
            return (T) new WorkoutListViewModel(container);
        }
        if (modelClass.isAssignableFrom(PlayerViewModel.class)) {
            return (T) new PlayerViewModel(container, workoutId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }
}
