package com.wob.workout.ui.workout;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;

import java.util.List;

public class WorkoutListItem {

    static final int MAX_STRIP_PICTURES = 5;

    public final Workout workout;
    public final int exerciseCount;
    public final int totalDurationSeconds;
    /** The exercises with a demonstration of their own, in workout order. */
    public final List<Exercise> picturedExercises;

    public WorkoutListItem(
            Workout workout,
            int exerciseCount,
            int totalDurationSeconds,
            List<Exercise> picturedExercises) {

        this.workout = workout;
        this.exerciseCount = exerciseCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.picturedExercises = picturedExercises;
    }

    public List<Exercise> stripExercises() {
        return picturedExercises.subList(
                0, Math.min(MAX_STRIP_PICTURES, picturedExercises.size()));
    }

    /** How many pictured exercises don't fit into the strip. */
    public int morePictures() {
        return Math.max(0, picturedExercises.size() - MAX_STRIP_PICTURES);
    }
}
