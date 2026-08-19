package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
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
public class WorkoutExercise {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long workoutId;

    public long exerciseId;

    public int position;

    public int durationSeconds;
}
