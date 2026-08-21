package com.wob.workout.ui.workout;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.data.repository.WorkoutSummaryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Establishes the ViewModel-migration testing pattern (in-memory Room database,
 * InstantTaskExecutorRule) that later screens reuse. Exercises WorkoutSummaryRepository
 * directly rather than constructing WorkoutListViewModel/AppContainer: the ViewModel is
 * a one-line passthrough (getWorkouts() just returns repository.observeAll()), so the
 * repository's LiveData is exactly what the ViewModel exposes.
 */
@RunWith(AndroidJUnit4.class)
public class WorkoutListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void workoutsReflectsSavedWorkoutAndItsExercises() throws InterruptedException {
        Workout workout = new Workout();
        workout.name = "Leg Day";
        long workoutId = database.workoutDao().insert(workout);

        Exercise squat = new Exercise();
        squat.title = "Squat";
        long exerciseId = database.exerciseDao().insert(squat);

        WorkoutExercise exercise = new WorkoutExercise();
        exercise.workoutId = workoutId;
        exercise.exerciseId = exerciseId;
        exercise.position = 0;
        exercise.durationSeconds = 45;
        database.workoutExerciseDao().insert(exercise);

        WorkoutSummaryRepository repository = new WorkoutSummaryRepository(database);

        List<WorkoutListItem> items = getOrAwaitValue(repository.observeAll());

        assertEquals(1, items.size());
        assertEquals("Leg Day", items.get(0).workout.name);
        assertEquals(1, items.get(0).exerciseCount);
        assertEquals(45, items.get(0).totalDurationSeconds);
    }

    @Test
    public void workoutsUpdatesWhenAnExerciseIsAdded() throws InterruptedException {
        Workout workout = new Workout();
        workout.name = "Push Day";
        long workoutId = database.workoutDao().insert(workout);

        Exercise pushup = new Exercise();
        pushup.title = "Push-up";
        long exerciseId = database.exerciseDao().insert(pushup);

        WorkoutSummaryRepository repository = new WorkoutSummaryRepository(database);

        List<WorkoutListItem> beforeAdding = getOrAwaitValue(repository.observeAll());
        assertEquals(0, beforeAdding.get(0).exerciseCount);

        WorkoutExercise exercise = new WorkoutExercise();
        exercise.workoutId = workoutId;
        exercise.exerciseId = exerciseId;
        exercise.position = 0;
        exercise.durationSeconds = 30;
        database.workoutExerciseDao().insert(exercise);

        List<WorkoutListItem> afterAdding = getOrAwaitValue(repository.observeAll());
        assertEquals(1, afterAdding.get(0).exerciseCount);
        assertEquals(30, afterAdding.get(0).totalDurationSeconds);
    }
}
