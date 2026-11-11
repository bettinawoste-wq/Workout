package com.wob.workout.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.ui.workout.WorkoutListItem;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final LiveData<List<WorkoutListItem>> recentWorkouts;

    public HomeViewModel(AppContainer container) {
        recentWorkouts = container.workoutSummaryRepository.observeRecent();
    }

    /** The recent workouts for the home screen's Workouts section, kept live. */
    public LiveData<List<WorkoutListItem>> getRecentWorkouts() {
        return recentWorkouts;
    }
}
