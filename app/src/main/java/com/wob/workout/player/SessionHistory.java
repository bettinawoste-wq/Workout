package com.wob.workout.player;

import com.wob.workout.data.WorkoutExercise;

/**
 * Where a {@link WorkoutSessionEngine} writes its workout session and exercise sessions.
 * {@link DatabaseSessionHistory} in the app; tests keep them in memory. Called in this
 * order: {@link #startSession} once, {@link #recordExercise} for each exercise session,
 * {@link #finishSession} at most once.
 */
public interface SessionHistory {

    void startSession(long workoutId, String workoutName);

    /**
     * @param position      where the exercise stands in the workout
     * @param actualSeconds how long it ran, pauses left out
     */
    void recordExercise(int position, WorkoutExercise workoutExercise, int actualSeconds,
                        long startedAt, long finishedAt);

    void finishSession(boolean completed);
}
