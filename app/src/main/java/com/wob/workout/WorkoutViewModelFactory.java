package com.wob.workout;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.wob.workout.player.PlayerViewModel;
import com.wob.workout.ui.exercise.ExerciseEditViewModel;
import com.wob.workout.ui.exercise.ExerciseListViewModel;
import com.wob.workout.ui.history.HistoryListViewModel;
import com.wob.workout.ui.workout.WorkoutEditViewModel;
import com.wob.workout.ui.workout.WorkoutListViewModel;

/**
 * One generic factory for the whole app: add one branch per ViewModel as screens
 * migrate, rather than a bespoke Factory class for each one. See wayfinder ticket #2.
 *
 * <p>Screens whose ViewModel needs a navigation argument (e.g. a workout or exercise id)
 * pass it to the constructor here as {@code navArgId} and it's threaded through in the
 * matching branch below. One field is enough since no screen so far needs more than one
 * such id — revisit this shape if that stops being true.
 */
public class WorkoutViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;
    private final long navArgId;

    public WorkoutViewModelFactory(AppContainer container) {
        this(container, -1);
    }

    public WorkoutViewModelFactory(AppContainer container, long navArgId) {
        this.container = container;
        this.navArgId = navArgId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WorkoutListViewModel.class)) {
            return (T) new WorkoutListViewModel(container);
        }
        if (modelClass.isAssignableFrom(PlayerViewModel.class)) {
            return (T) new PlayerViewModel(container, navArgId);
        }
        if (modelClass.isAssignableFrom(WorkoutEditViewModel.class)) {
            return (T) new WorkoutEditViewModel(container, navArgId);
        }
        if (modelClass.isAssignableFrom(ExerciseListViewModel.class)) {
            return (T) new ExerciseListViewModel(container);
        }
        if (modelClass.isAssignableFrom(ExerciseEditViewModel.class)) {
            return (T) new ExerciseEditViewModel(container, navArgId);
        }
        if (modelClass.isAssignableFrom(HistoryListViewModel.class)) {
            return (T) new HistoryListViewModel(container);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }
}
