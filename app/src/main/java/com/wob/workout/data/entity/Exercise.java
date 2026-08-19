package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    public String description;

    public String mediaFile;

    public MediaType mediaType;
}
