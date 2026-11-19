package com.wob.workout.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.training.Clock;
import com.wob.workout.training.WeeklyProgress;
import com.wob.workout.ui.workout.WorkoutListItem;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final LiveData<List<WorkoutListItem>> recentWorkouts;
    private final MediatorLiveData<WeeklyProgress> weeklyProgress = new MediatorLiveData<>();
    private final Clock clock;

    private List<WorkoutSession> sessions;
    private List<WeeklyGoalChange> goalChanges;

    public HomeViewModel(AppContainer container) {
        clock = container.clock;
        recentWorkouts = container.workoutSummaryRepository.observeRecent();
        weeklyProgress.addSource(container.workoutHistoryRepository.observeSessions(), all -> {
            sessions = all;
            updateWeeklyProgress();
        });
        weeklyProgress.addSource(container.weeklyGoalRepository.observeChanges(), all -> {
            goalChanges = all;
            updateWeeklyProgress();
        });
    }

    /** The recent workouts for the home screen's Workouts section, kept live. */
    public LiveData<List<WorkoutListItem>> getRecentWorkouts() {
        return recentWorkouts;
    }

    /** The weekly goal's progress and streak as of now, worked out again whenever the
     *  history or the goal changes. */
    public LiveData<WeeklyProgress> getWeeklyProgress() {
        return weeklyProgress;
    }

    private void updateWeeklyProgress() {
        if (sessions == null || goalChanges == null) return;
        weeklyProgress.setValue(WeeklyProgress.of(sessions, goalChanges, clock));
    }
}
