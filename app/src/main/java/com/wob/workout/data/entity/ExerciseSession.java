package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        foreignKeys = {
                @ForeignKey(
                        entity = WorkoutSession.class,
                        parentColumns = "id",
                        childColumns = "sessionId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("sessionId")
        }
)
public class ExerciseSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;

    public long exerciseId;

    public String exerciseTitle;

    public int position;

    public int plannedDurationSeconds;

    public int actualDurationSeconds;

    public long startedAt;

    public long finishedAt;
}
