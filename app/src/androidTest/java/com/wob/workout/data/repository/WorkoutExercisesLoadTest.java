package com.wob.workout.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.TestAppContainer;
import com.wob.workout.data.WorkoutExercise;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * {@link WorkoutRepository#getWorkoutExercises} against Room. Leaving out a workout exercise
 * whose exercise is missing is covered on the JVM ({@code WorkoutRepositoryTest}): the
 * foreign key keeps such a row out of a real database.
 */
@RunWith(AndroidJUnit4.class)
public class WorkoutExercisesLoadTest {

    private TestAppContainer testContainer;
    private WorkoutRepository repository;

    @Before
    public void setUp() {
        testContainer = new TestAppContainer();
        repository = testContainer.getAppContainer().workoutRepository;
    }

    @After
    public void tearDown() {
        testContainer.close();
    }

    @Test
    public void loadsTheWorkoutExercisesInTheirOrder() {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        long lungeRowId = testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, squatId, 2, 60);

        List<WorkoutExercise> loaded = repository.getWorkoutExercises(workoutId);

        assertEquals(3, loaded.size());
        assertEquals("Squat", loaded.get(0).exercise.title);
        assertEquals(45, loaded.get(0).durationSeconds);
        assertEquals("Lunge", loaded.get(1).exercise.title);
        assertEquals(lungeRowId, loaded.get(1).rowId);
        assertEquals("Squat", loaded.get(2).exercise.title);
        assertEquals(60, loaded.get(2).durationSeconds);
    }

    @Test
    public void aWorkoutThatIsGoneHasNoWorkoutExercises() {
        assertTrue(repository.getWorkoutExercises(404).isEmpty());
    }
}
