package com.wob.workout.data.entity;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WorkoutTest {

    @Test
    public void aNewWorkoutFollowsTheDefaultTransition() {
        assertNull(new Workout().transitionSeconds);
    }
}
