package com.wob.workout.data.relation;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A Workout together with its exercises, fetched via Room's batched relation query
 * (one query for workouts, one batched IN(...) query for their exercises) instead of
 * one query per workout. See wayfinder ticket #3.
 */
public class WorkoutWithExercises {

    @Embedded
    public Workout workout;

    /** Not ordered — Room's @Relation batched fetch doesn't preserve any particular
     *  order. Use {@link #exercisesInOrder()} wherever the workout's configured
     *  exercise order matters. */
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    public List<WorkoutExercise> exercises;

    /** The exercises behind {@link #exercises}: one per distinct exercise, in no particular
     *  order. Look them up by id when walking {@link #exercisesInOrder()}. */
    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(
                    value = WorkoutExercise.class,
                    parentColumn = "workoutId",
                    entityColumn = "exerciseId"))
    public List<Exercise> exerciseDetails;

    public List<WorkoutExercise> exercisesInOrder() {
        List<WorkoutExercise> sorted = new ArrayList<>(exercises);
        sorted.sort(Comparator.comparingInt(exercise -> exercise.position));
        return sorted;
    }
}
