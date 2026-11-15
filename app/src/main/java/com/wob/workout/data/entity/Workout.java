package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Workout {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    /** 3 for every workout; {@code AppDatabase.MIGRATION_5_6} set existing ones to it. */
    public int transitionSeconds = 3;

    public boolean countdownEnabled = true;
}