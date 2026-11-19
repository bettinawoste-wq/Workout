package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * The stored row behind a {@link com.wob.workout.data.WorkoutExercise}: one place in a
 * workout, an exercise's or a rest's (no exercise). Keeps the table name
 * {@code WorkoutExercise}.
 */
@Entity(
        tableName = "WorkoutExercise",
        foreignKeys = {
                @ForeignKey(
                        entity = Workout.class,
                        parentColumns = "id",
                        childColumns = "workoutId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Exercise.class,
                        parentColumns = "id",
                        childColumns = "exerciseId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("workoutId"),
                @Index("exerciseId")
        }
)
public class WorkoutExerciseRow {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long workoutId;

    /** Null for a rest. */
    public Long exerciseId;

    public int position;

    public int durationSeconds;
}
