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

    /** How long it rested, pauses left out, 0 when every rest was ended at once; null when
     *  no rest ran, and for a workout session recorded before rests existed. */
    public Integer restSeconds;
}
