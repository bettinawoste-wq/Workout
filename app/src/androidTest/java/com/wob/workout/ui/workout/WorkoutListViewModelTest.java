package com.wob.workout.ui.workout;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static com.wob.workout.LiveDataTestUtil.observeValues;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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
    private WorkoutSummaryRepository repository;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new WorkoutSummaryRepository(database);
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    private long insertWorkout(String name) {
        Workout workout = new Workout();
        workout.name = name;
        return database.workoutDao().insert(workout);
    }

    private long insertExercise(String title) {
        Exercise exercise = new Exercise();
        exercise.title = title;
        return database.exerciseDao().insert(exercise);
    }

    private void insertWorkoutExercise(long workoutId, long exerciseId, int durationSeconds) {
        WorkoutExercise workoutExercise = new WorkoutExercise();
        workoutExercise.workoutId = workoutId;
        workoutExercise.exerciseId = exerciseId;
        workoutExercise.position = 0;
        workoutExercise.durationSeconds = durationSeconds;
        database.workoutExerciseDao().insert(workoutExercise);
    }

    @Test
    public void workoutsReflectsSavedWorkoutAndItsExercises() throws InterruptedException {
        long workoutId = insertWorkout("Leg Day");
        long exerciseId = insertExercise("Squat");
        insertWorkoutExercise(workoutId, exerciseId, 45);

        List<WorkoutListItem> items = getOrAwaitValue(repository.observeAll());

        assertEquals(1, items.size());
        assertEquals("Leg Day", items.get(0).workout.name);
        assertEquals(1, items.get(0).exerciseCount);
        assertEquals(45, items.get(0).totalDurationSeconds);
    }

    @Test
    public void workoutsUpdatesWhenAnExerciseIsAdded() throws InterruptedException {
        long workoutId = insertWorkout("Push Day");
        long exerciseId = insertExercise("Push-up");

        // One still-active observer across the mutation below - this is what actually
        // proves Room's invalidation tracking replaces the deleted onResume()/reload,
        // as opposed to two independent one-shot reads that would pass even if nothing
        // re-notified an existing observer.
        BlockingQueue<List<WorkoutListItem>> values = observeValues(repository.observeAll());

        List<WorkoutListItem> beforeAdding = values.poll(2, TimeUnit.SECONDS);
        assertEquals(0, beforeAdding.get(0).exerciseCount);

        insertWorkoutExercise(workoutId, exerciseId, 30);

        List<WorkoutListItem> afterAdding = values.poll(2, TimeUnit.SECONDS);
        assertEquals(1, afterAdding.get(0).exerciseCount);
        assertEquals(30, afterAdding.get(0).totalDurationSeconds);
    }
}
