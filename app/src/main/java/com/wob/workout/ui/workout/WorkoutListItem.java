package com.wob.workout.ui.workout;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;

import java.util.List;

public class WorkoutListItem {

    public final Workout workout;
    public final int exerciseCount;
    public final int totalDurationSeconds;
    /** The workout's exercises in workout order; one used twice appears twice. */
    public final List<Exercise> exercises;

    public WorkoutListItem(
            Workout workout,
            int exerciseCount,
            int totalDurationSeconds,
            List<Exercise> exercises) {

        this.workout = workout;
        this.exerciseCount = exerciseCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.exercises = exercises;
    }
}
