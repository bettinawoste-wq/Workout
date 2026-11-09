package com.wob.workout.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WorkoutListItemTest {

    @Test
    public void noPicturesMeansAnEmptyStrip() {
        WorkoutListItem item = itemWithPictures(0);

        assertTrue(item.stripExercises().isEmpty());
        assertEquals(0, item.morePictures());
    }

    @Test
    public void fivePicturesFitWithoutAnOverflow() {
        WorkoutListItem item = itemWithPictures(5);

        assertEquals(5, item.stripExercises().size());
        assertEquals(0, item.morePictures());
    }

    @Test
    public void morePicturesThanSlotsShowsTheFirstFiveAndCountsTheRest() {
        WorkoutListItem item = itemWithPictures(8);

        assertEquals(5, item.stripExercises().size());
        assertEquals("exercise 0", item.stripExercises().get(0).title);
        assertEquals("exercise 4", item.stripExercises().get(4).title);
        assertEquals(3, item.morePictures());
    }

    private static WorkoutListItem itemWithPictures(int count) {
        List<Exercise> pictured = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Exercise exercise = new Exercise();
            exercise.title = "exercise " + i;
            pictured.add(exercise);
        }
        return new WorkoutListItem(new Workout(), count, 0, pictured);
    }
}
