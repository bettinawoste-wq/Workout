package com.wob.workout.player;

import com.wob.workout.data.entity.Exercise;

public class PlayerExercise {

    public Exercise exercise;
    public int durationSeconds;

    public PlayerExercise(Exercise exercise, int durationSeconds) {
        this.exercise = exercise;
        this.durationSeconds = durationSeconds;
    }
}