package com.wob.workout.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;

import java.util.List;

public class WorkoutListViewModel extends ViewModel {

    private final LiveData<List<WorkoutListItem>> workouts;

    public WorkoutListViewModel(AppContainer container) {
        workouts = container.workoutSummaryRepository.observeAll();
    }

    public LiveData<List<WorkoutListItem>> getWorkouts() {
        return workouts;
    }
}
