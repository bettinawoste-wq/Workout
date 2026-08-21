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

    public int activeDurationSeconds;

    public boolean completed;
}
