package com.wob.workout.player;

import com.wob.workout.data.repository.WorkoutHistoryRepository;

/**
 * Owns the same history-tracking state {@code PlayerFragment} used to hold directly —
 * relocated here so it survives configuration changes with the ViewModel and is
 * unit-testable on its own (pure state plus one repository dependency, no Android
 * framework classes involved). See wayfinder ticket #4.
 */
public class PlayerHistoryTracker {

    private final WorkoutHistoryRepository historyRepository;

    private long sessionId = -1;
    private boolean sessionClosed = false;
    private boolean exerciseStarted = false;
    private long currentExerciseStartedAt;
    private long currentExercisePausedMillis;
    private long pauseStartedAt = 0;

    public PlayerHistoryTracker(WorkoutHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void startSession(long workoutId, String workoutName) {
        sessionId = historyRepository.startSession(workoutId, workoutName);
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
        if (pauseStartedAt != 0) {
            currentExercisePausedMillis += System.currentTimeMillis() - pauseStartedAt;
            pauseStartedAt = 0;
        }
    }

    public void closeOutCurrentExercise(int index, PlayerExercise current) {
        if (!exerciseStarted) return;

        long now = System.currentTimeMillis();
        long pausedMillis = currentExercisePausedMillis;
        if (pauseStartedAt != 0) {
            pausedMillis += now - pauseStartedAt;
            pauseStartedAt = 0;
        }

        long activeMillis = Math.max(0, now - currentExerciseStartedAt - pausedMillis);

        historyRepository.recordExercise(
                sessionId,
                current.exercise.id,
                current.exercise.title,
                index,
                current.durationSeconds,
                (int) (activeMillis / 1000),
                currentExerciseStartedAt,
                now);

        exerciseStarted = false;
    }

    public void finish(boolean completed) {
        if (sessionId < 0 || sessionClosed) return;
        historyRepository.finishSession(sessionId, completed);
        sessionClosed = true;
    }
}
