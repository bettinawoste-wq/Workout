package com.wob.workout.prototype;

// PROTOTYPE — throwaway, see AppContainer.java for context.
// Demonstrates the pattern end-to-end against one real, trivial existing call:
// WorkoutRepository.getAll(), which today runs as a blocking Room query.

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.data.entity.Workout;

import java.util.List;

public class WorkoutListViewModel extends ViewModel {

    private final LiveData<List<Workout>> workouts;

    public WorkoutListViewModel(AppContainer container) {
        workouts = LiveDataExecutor.runOnExecutor(
                container.ioExecutor,
                () -> container.workoutRepository.getAll());
    }

    public LiveData<List<Workout>> getWorkouts() {
        return workouts;
    }
}
