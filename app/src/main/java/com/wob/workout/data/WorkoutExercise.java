package com.wob.workout.data;

import com.wob.workout.data.entity.Exercise;

import java.util.List;

/**
 * One place inside a workout: an exercise and how long it lasts there, or a rest, which has
 * a duration and no exercise. Its position is where it stands in its workout's list.
 * Immutable: a change makes a new value.
 *
 * <p>The workout editor and the player both work with it; both load it through
 * {@code WorkoutRepository.getWorkoutExercises}. What is stored is a
 * {@link com.wob.workout.data.entity.WorkoutExerciseRow}.
 */
public final class WorkoutExercise {

    /** The stored row's id, or {@link #UNSAVED} for one added in the editor and not saved. */
    public final long rowId;
    /** Null for a rest. */
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

    /** A rest, not yet stored. */
    public static WorkoutExercise addedRest(int durationSeconds) {
        return new WorkoutExercise(UNSAVED, null, durationSeconds);
    }

    public boolean isRest() {
        return exercise == null;
    }

    public WorkoutExercise withDuration(int durationSeconds) {
        return new WorkoutExercise(rowId, exercise, durationSeconds);
    }

    /** The same place, showing the exercise as the catalog has it now. */
    public WorkoutExercise withExercise(Exercise exercise) {
        return new WorkoutExercise(rowId, exercise, durationSeconds);
    }

    /** Null for a rest. */
    public Long exerciseId() {
        return exercise != null ? exercise.id : null;
    }

    /** Whether the rest at {@code position} is skipped: one at the start or the end (no
     *  exercise before or after it) or right after another rest. Only a rest between two
     *  exercises runs, and of several rests in a row only the first. False for an
     *  exercise. */
    public static boolean isSkippedRest(List<WorkoutExercise> places, int position) {
        if (!places.get(position).isRest()) return false;
        if (position == 0 || places.get(position - 1).isRest()) return true;
        for (int i = position + 1; i < places.size(); i++) {
            if (!places.get(i).isRest()) return false;
        }
        return true;
    }
}
