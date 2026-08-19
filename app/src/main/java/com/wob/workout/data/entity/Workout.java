package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Workout {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    public int transitionSeconds = 5;

    public boolean countdownEnabled = true;
}