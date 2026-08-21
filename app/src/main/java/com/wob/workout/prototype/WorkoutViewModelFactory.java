package com.wob.workout.prototype;

// PROTOTYPE — throwaway, see AppContainer.java for context.
// One generic factory for the whole app: add one branch per ViewModel as screens
// migrate, rather than writing a bespoke Factory class for each one.

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class WorkoutViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;

    public WorkoutViewModelFactory(AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WorkoutListViewModel.class)) {
            return (T) new WorkoutListViewModel(container);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }

    // Fragment usage sketch (not part of this prototype):
    //
    // AppContainer container = ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
    // WorkoutListViewModel vm = new ViewModelProvider(this, new WorkoutViewModelFactory(container))
    //         .get(WorkoutListViewModel.class);
    // vm.getWorkouts().observe(getViewLifecycleOwner(), workouts -> adapter.setData(...));
}
