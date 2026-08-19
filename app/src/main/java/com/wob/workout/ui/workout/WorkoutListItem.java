package com.wob.workout.ui.workout;

import com.wob.workout.data.entity.Workout;

public class WorkoutListItem {

    public final Workout workout;
    public final int exerciseCount;
    public final int totalDurationSeconds;

    public WorkoutListItem(
            Workout workout,
            int exerciseCount,
            int totalDurationSeconds) {

        this.workout = workout;
        this.exerciseCount = exerciseCount;
        this.totalDurationSeconds = totalDurationSeconds;
    }
}