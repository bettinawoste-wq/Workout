package com.wob.workout.player;

import android.util.Log;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.repository.WorkoutHistoryRepository;

import java.util.concurrent.Executor;

/**
 * {@link SessionHistory} in the database, through {@link WorkoutHistoryRepository}.
 *
 * <p>Threading (issue #21): called on the main thread; each write is handed to
 * {@code ioExecutor}. That executor must be <b>single-threaded</b>
 * ({@code AppContainer.ioExecutor} is): it keeps the writes in call order, so
 * {@link #sessionId} - assigned by the {@code startSession} task - is already set when a
 * later {@code recordExercise}/{@code finishSession} task reads it, and both of those only
 * ever touch it on that one executor thread. The executor outlives the ViewModel, so the
 * close-out writes from {@code onCleared()} still land.
 */
public class DatabaseSessionHistory implements SessionHistory {

    private static final String TAG = "DatabaseSessionHistory";

    private final WorkoutHistoryRepository historyRepository;
    private final Executor ioExecutor;

    /** Only read and written on the {@code ioExecutor} thread - see the class doc. */
    private long sessionId = -1;

    public DatabaseSessionHistory(WorkoutHistoryRepository historyRepository,
                                  Executor ioExecutor) {
        this.historyRepository = historyRepository;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public void startSession(long workoutId, String workoutName) {
        write(() -> sessionId = historyRepository.startSession(workoutId, workoutName));
    }

    @Override
    public void recordExercise(int position, WorkoutExercise workoutExercise,
                               int actualSeconds, long startedAt, long finishedAt) {
        long exerciseId = workoutExercise.exerciseId();
        String title = workoutExercise.exercise.title;
        int plannedSeconds = workoutExercise.durationSeconds;
        write(() -> {
            if (sessionId < 0) return; // startSession's insert failed - nothing to attach to
            historyRepository.recordExercise(sessionId, exerciseId, title, position,
                    plannedSeconds, actualSeconds, startedAt, finishedAt);
        });
    }

    @Override
    public void finishSession(boolean completed) {
        write(() -> {
            if (sessionId < 0) return;
            historyRepository.finishSession(sessionId, completed);
        });
    }

    /** A throw on a raw executor thread would kill the app process, so failures are
     *  logged instead. Catches {@code Exception}, not just {@code RuntimeException}: Room's
     *  blocking DAO calls can rethrow a checked {@code InterruptedException} undeclared. */
    private void write(Runnable write) {
        ioExecutor.execute(() -> {
            try {
                write.run();
            } catch (Exception e) {
                Log.e(TAG, "History write failed", e);
            }
        });
    }
}
