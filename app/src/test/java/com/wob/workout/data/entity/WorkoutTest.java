package com.wob.workout.data.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WorkoutTest {

    @Test
    public void aNewWorkoutsTransitionLastsThreeSeconds() {
        assertEquals(3, new Workout().transitionSeconds);
    }
}
