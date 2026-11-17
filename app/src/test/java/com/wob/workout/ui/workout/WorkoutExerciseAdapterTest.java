package com.wob.workout.ui.workout;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/** Which list changes reach the editor's list as a move, so that a drag goes on. */
public class WorkoutExerciseAdapterTest {

    private final WorkoutExercise a = workoutExercise("A");
    private final WorkoutExercise b = workoutExercise("B");
    private final WorkoutExercise c = workoutExercise("C");
    private final WorkoutExercise d = workoutExercise("D");

    @Test
    public void oneMovedDownIsAMove() {
        assertArrayEquals(new int[] {0, 2},
                WorkoutExerciseAdapter.singleMove(list(a, b, c, d), list(b, c, a, d)));
    }

    @Test
    public void oneMovedUpIsAMove() {
        assertArrayEquals(new int[] {3, 1},
                WorkoutExerciseAdapter.singleMove(list(a, b, c, d), list(a, d, b, c)));
    }

    @Test
    public void neighboursSwappedIsAMove() {
        assertArrayEquals(new int[] {1, 2},
                WorkoutExerciseAdapter.singleMove(list(a, b, c), list(a, c, b)));
    }

    @Test
    public void anyOtherChangeIsNotAMove() {
        assertNull(WorkoutExerciseAdapter.singleMove(list(a, b, c), list(a, b, c)));
        assertNull(WorkoutExerciseAdapter.singleMove(list(a, b, c), list(c, b, a)));
        assertNull(WorkoutExerciseAdapter.singleMove(list(a, b), list(a, b, c)));
        assertNull(WorkoutExerciseAdapter.singleMove(
                list(a, b), list(a, b.withDuration(99))));
    }

    private static List<WorkoutExercise> list(WorkoutExercise... workoutExercises) {
        return Arrays.asList(workoutExercises);
    }

    private static WorkoutExercise workoutExercise(String title) {
        Exercise exercise = new Exercise();
        exercise.title = title;
        return WorkoutExercise.added(exercise, 60);
    }
}
