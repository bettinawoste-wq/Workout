package com.wob.workout.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.WorkoutSession;

import java.util.List;

public class HistoryListViewModel extends ViewModel {

    private final AppContainer container;
    private final LiveData<List<WorkoutSessionSummary>> history;

    public HistoryListViewModel(AppContainer container) {
        this.container = container;
        this.history = container.workoutHistoryRepository.observeHistory();
    }

    public LiveData<List<WorkoutSessionSummary>> getHistory() {
        return history;
    }

    /** No return value — Room's invalidation naturally re-emits {@link #getHistory()}
     *  without the deleted session, so the Fragment doesn't need a completion signal. Still
     *  routed through LiveDataExecutor rather than a raw Runnable so a failure is caught and
     *  logged instead of crashing the executor thread silently. */
    public void delete(WorkoutSession session) {
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            container.workoutHistoryRepository.delete(session);
            return null;
        }, result -> { });
    }
}
