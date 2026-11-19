package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        indices = {
                @Index("workoutId")
        }
)
public class WorkoutSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long workoutId;

    public String workoutName;

    public long startedAt;

    public long finishedAt;

    /** Exercise time only: rests and transitions are not part of it. */
    public int activeDurationSeconds;

    public boolean completed;

    /** How long it rested, pauses left out; null for a workout session recorded before
     *  rests existed. */
    public Integer restSeconds;
}
