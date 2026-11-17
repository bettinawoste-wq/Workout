package com.wob.workout.data;

import com.wob.workout.data.entity.Exercise;

/**
 * One exercise's place inside a workout: the exercise and how long it lasts there. Its
 * position is where it stands in its workout's list. Immutable: a change makes a new value.
 *
 * <p>The workout editor and the player both work with it; both load it through
 * {@code WorkoutRepository.getWorkoutExercises}. What is stored is a
 * {@link com.wob.workout.data.entity.WorkoutExerciseRow}.
 */
public final class WorkoutExercise {

    /** The stored row's id, or {@link #UNSAVED} for one added in the editor and not saved. */
    public final long rowId;
    public final Exercise exercise;
    public final int durationSeconds;

    public static final long UNSAVED = 0;

    public WorkoutExercise(long rowId, Exercise exercise, int durationSeconds) {
        this.rowId = rowId;
        this.exercise = exercise;
        this.durationSeconds = durationSeconds;
    }

    /** Not yet stored. */
    public static WorkoutExercise added(Exercise exercise, int durationSeconds) {
        return new WorkoutExercise(UNSAVED, exercise, durationSeconds);
    }

    public WorkoutExercise withDuration(int durationSeconds) {
        return new WorkoutExercise(rowId, exercise, durationSeconds);
    }

    /** The same place, showing the exercise as the catalog has it now. */
    public WorkoutExercise withExercise(Exercise exercise) {
        return new WorkoutExercise(rowId, exercise, durationSeconds);
    }

    public long exerciseId() {
        return exercise.id;
    }
}
