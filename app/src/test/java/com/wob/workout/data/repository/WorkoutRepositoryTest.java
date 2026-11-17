package com.wob.workout.data.repository;

import static org.junit.Assert.assertEquals;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExerciseRow;
import com.wob.workout.data.relation.WorkoutWithExercises;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The rule behind {@link WorkoutRepository#getWorkoutExercises}, on what Room hands it. */
public class WorkoutRepositoryTest {

    @Test
    public void workoutExercisesComeInTheirPositionsOrder() {
        Exercise squat = exercise(1, "Squat");
        Exercise lunge = exercise(2, "Lunge");
        WorkoutWithExercises stored = stored(
                Arrays.asList(row(11, 2, 1, 30), row(10, 1, 0, 45)),
                Arrays.asList(lunge, squat));

        List<WorkoutExercise> loaded = WorkoutRepository.workoutExercisesOf(stored);

        assertEquals(2, loaded.size());
        assertEquals("Squat", loaded.get(0).exercise.title);
        assertEquals(45, loaded.get(0).durationSeconds);
        assertEquals(10, loaded.get(0).rowId);
        assertEquals("Lunge", loaded.get(1).exercise.title);
        assertEquals(30, loaded.get(1).durationSeconds);
        assertEquals(11, loaded.get(1).rowId);
    }

    @Test
    public void theSameExerciseTwiceGivesTwoWorkoutExercises() {
        Exercise plank = exercise(3, "Plank");
        WorkoutWithExercises stored = stored(
                Arrays.asList(row(20, 3, 0, 30), row(21, 3, 1, 60)), Arrays.asList(plank));

        List<WorkoutExercise> loaded = WorkoutRepository.workoutExercisesOf(stored);

        assertEquals(2, loaded.size());
        assertEquals(30, loaded.get(0).durationSeconds);
        assertEquals(60, loaded.get(1).durationSeconds);
    }

    @Test
    public void aWorkoutExerciseWhoseExerciseIsMissingIsLeftOut() {
        Exercise lunge = exercise(2, "Lunge");
        WorkoutWithExercises stored = stored(
                Arrays.asList(row(10, 1, 0, 45), row(11, 2, 1, 30)), Arrays.asList(lunge));

        List<WorkoutExercise> loaded = WorkoutRepository.workoutExercisesOf(stored);

        assertEquals(1, loaded.size());
        assertEquals("Lunge", loaded.get(0).exercise.title);
    }

    private static WorkoutWithExercises stored(List<WorkoutExerciseRow> rows,
                                               List<Exercise> exercises) {
        WorkoutWithExercises stored = new WorkoutWithExercises();
        stored.exercises = new ArrayList<>(rows);
        stored.exerciseDetails = new ArrayList<>(exercises);
        return stored;
    }

    private static WorkoutExerciseRow row(long id, long exerciseId, int position, int seconds) {
        WorkoutExerciseRow row = new WorkoutExerciseRow();
        row.id = id;
        row.exerciseId = exerciseId;
        row.position = position;
        row.durationSeconds = seconds;
        return row;
    }

    private static Exercise exercise(long id, String title) {
        Exercise exercise = new Exercise();
        exercise.id = id;
        exercise.title = title;
        return exercise;
    }
}
