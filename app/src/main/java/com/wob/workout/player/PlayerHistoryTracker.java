package com.wob.workout.player;

import android.util.Log;

import com.wob.workout.data.repository.WorkoutHistoryRepository;

import java.util.concurrent.Executor;

/**
 * Owns the same history-tracking state {@code PlayerFragment} used to hold directly —
 * relocated here so it survives configuration changes with the ViewModel and is
 * testable on its own (pure state plus one repository dependency and an executor; the
 * only framework use is {@code android.util.Log} on the error path, so its test stays
 * instrumented). See wayfinder ticket #4.
 *
 * <p>Threading (issue #21): every public method is called from the main thread and does
 * its time/pause bookkeeping there, capturing the values it needs; only the repository
 * write is handed to {@code ioExecutor}. That executor must be <b>single-threaded</b>
 * ({@code AppContainer.ioExecutor} is): it keeps the writes in call order, so
 * {@link #sessionId} - assigned by the {@code startSession} task - is already set when a
 * later {@code recordExercise}/{@code finishSession} task reads it, and both of those only
 * ever touch it on that one executor thread. The executor outlives the ViewModel, so the
 * close-out writes from {@code onCleared()} still land.
 */
public class PlayerHistoryTracker {

    private static final String TAG = "PlayerHistoryTracker";

    private final WorkoutHistoryRepository historyRepository;
    private final Executor ioExecutor;

    /** Only read and written on the {@code ioExecutor} thread - see the class doc. */
    private long sessionId = -1;

    // Main-thread state.
    private boolean sessionStarted = false;
    private boolean sessionClosed = false;
    private boolean exerciseStarted = false;
    private long currentExerciseStartedAt;
    private long currentExercisePausedMillis;
    private long pauseStartedAt = 0;
    private int activeSeconds = 0;

    public PlayerHistoryTracker(WorkoutHistoryRepository historyRepository, Executor ioExecutor) {
        this.historyRepository = historyRepository;
        this.ioExecutor = ioExecutor;
    }

    public void startSession(long workoutId, String workoutName) {
        sessionStarted = true;
        write(() -> sessionId = historyRepository.startSession(workoutId, workoutName));
    }

    public void startExercise() {
        currentExerciseStartedAt = System.currentTimeMillis();
        currentExercisePausedMillis = 0;
        pauseStartedAt = 0;
        exerciseStarted = true;
    }

    public void onExercisePause() {
        pauseStartedAt = System.currentTimeMillis();
    }

    public void onExerciseResume() {
        flushPause(System.currentTimeMillis());
    }

    public void closeOutCurrentExercise(int index, PlayerExercise current) {
        if (!exerciseStarted) return;

        long now = System.currentTimeMillis();
        long pausedMillis = flushPause(now);
        long activeMillis = Math.max(0, now - currentExerciseStartedAt - pausedMillis);
        long startedAt = currentExerciseStartedAt;
        long exerciseId = current.exercise.id;
        String title = current.exercise.title;
        int plannedSeconds = current.durationSeconds;
        int actualSeconds = (int) (activeMillis / 1000);
        activeSeconds += actualSeconds;

        write(() -> {
            if (sessionId < 0) return; // startSession's insert failed - nothing to attach to
            historyRepository.recordExercise(
                    sessionId,
                    exerciseId,
                    title,
                    index,
                    plannedSeconds,
                    actualSeconds,
                    startedAt,
                    now);
        });

        exerciseStarted = false;
    }

    /** The same total the session's History entry ends up with: its recorded exercises added up. */
    public int getActiveSeconds() {
        return activeSeconds;
    }

    public void finish(boolean completed) {
        if (!sessionStarted || sessionClosed) return;
        sessionClosed = true;
        write(() -> {
            if (sessionId < 0) return;
            historyRepository.finishSession(sessionId, completed);
        });
    }

    /** Folds any pending pause into the accumulated paused time and returns the total. */
    private long flushPause(long now) {
        if (pauseStartedAt != 0) {
            currentExercisePausedMillis += now - pauseStartedAt;
            pauseStartedAt = 0;
        }
        return currentExercisePausedMillis;
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
