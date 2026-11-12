package com.wob.workout.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.WorkoutSession;

import java.util.List;

/**
 * One-shot load, not a reactive Room query — no relation/N+1 to fix here (two cheap
 * point-queries, not a loop), and nothing else can mutate this specific session while
 * it's open (only one Fragment is visible at a time in this single-activity app). See
 * wayfinder ticket #9.
 */
public class HistoryDetailViewModel extends ViewModel {

    private final MutableLiveData<HistoryDetail> detail = new MutableLiveData<>();

    public HistoryDetailViewModel(AppContainer container, long sessionId) {
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            WorkoutSession session = container.workoutHistoryRepository.getSession(sessionId);
            if (session == null) return null;

            List<ExerciseSession> exercises =
                    container.workoutHistoryRepository.getExercises(sessionId);
            return new HistoryDetail(session, exercises);
        }, result -> {
            if (result == null) return;
            detail.setValue(result);
        });
    }

    public LiveData<HistoryDetail> getDetail() {
        return detail;
    }

    public static class HistoryDetail {
        public final WorkoutSession session;
        public final List<ExerciseSession> exercises;

        HistoryDetail(WorkoutSession session, List<ExerciseSession> exercises) {
            this.session = session;
            this.exercises = exercises;
        }
    }
}
