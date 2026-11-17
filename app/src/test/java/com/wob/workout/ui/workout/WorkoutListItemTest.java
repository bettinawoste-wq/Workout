package com.wob.workout.ui.workout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.entity.Workout;
import java.util.Collections;
import org.junit.Test;

public class WorkoutListItemTest {

    @Test
    public void aWorkoutWithoutExercisesCannotStart() {
        WorkoutListItem item = new WorkoutListItem(new Workout(), 0, 0, Collections.emptyList());

        assertFalse(item.canStart());
    }

    @Test
    public void aWorkoutWithExercisesCanStart() {
        WorkoutListItem item = new WorkoutListItem(new Workout(), 1, 30, Collections.emptyList());

        assertTrue(item.canStart());
    }
}
