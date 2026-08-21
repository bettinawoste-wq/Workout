package com.wob.workout.data.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;

import java.util.List;

/**
 * A Workout together with its exercises, fetched via Room's batched relation query
 * (one query for workouts, one batched IN(...) query for their exercises) instead of
 * one query per workout. See wayfinder ticket #3.
 */
public class WorkoutWithExercises {

    @Embedded
    public Workout workout;

    @Relation(parentColumn = "id", entityColumn = "workoutId")
    public List<WorkoutExercise> exercises;
}
