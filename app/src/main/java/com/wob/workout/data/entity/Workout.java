package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Workout {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    /** The workout's own transition length, or null to follow the default transition
     *  ({@code DefaultTransitionSetting}); {@code AppDatabase.MIGRATION_6_7} made every
     *  existing workout follow it. */
    public Integer transitionSeconds;

    public boolean countdownEnabled = true;
}