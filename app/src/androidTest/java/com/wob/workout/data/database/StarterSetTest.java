package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.R;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.media.MediaStorageManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

/**
 * Runs against the debug variant, so the expected numbers are read from the same resources the
 * seeder reads rather than hard-coded; what the release variant ships is pinned separately by
 * StarterResourcesTest.
 */
@RunWith(AndroidJUnit4.class)
public class StarterSetTest {

    private static final String DB_NAME = "starter-set-test.db";

    private Context context;
    private Resources resources;
    private AppDatabase database;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        resources = context.getResources();
        context.deleteDatabase(DB_NAME);
        database = open();
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase(DB_NAME);
    }

    @Test
    public void freshDatabase_getsTheStarterExercises() {
        String[] titles = resources.getStringArray(R.array.starter_exercise_titles);
        String[] descriptions = resources.getStringArray(R.array.starter_exercise_descriptions);

        List<Exercise> exercises = database.exerciseDao().getAll();

        assertEquals(titles.length, exercises.size());
        // getAll() is newest-first, so the last starter exercise comes first.
        for (int i = 0; i < titles.length; i++) {
            Exercise exercise = exercises.get(titles.length - 1 - i);
            assertEquals(titles[i], exercise.title);
            assertEquals(descriptions[i], exercise.description);
        }
    }

    @Test
    public void onlyExercisesWithAConfiguredImageGetOneAndItsFileExists() {
        TypedArray images = resources.obtainTypedArray(R.array.starter_exercise_images);
        List<Exercise> exercises = database.exerciseDao().getAll();

        for (int i = 0; i < images.length(); i++) {
            Exercise exercise = exercises.get(images.length() - 1 - i);
            if (images.getResourceId(i, 0) == 0) {
                assertNull(exercise.mediaFile);
                assertNull(exercise.mediaType);
            } else {
                assertEquals(MediaType.IMAGE, exercise.mediaType);
                assertTrue(new File(MediaStorageManager.getFullPath(context, exercise.mediaFile))
                        .isFile());
            }
        }
        images.recycle();
    }

    @Test
    public void freshDatabase_getsOneStarterWorkoutWithTheDefaultTransition() {
        List<Workout> workouts = database.workoutDao().getAll();

        assertEquals(1, workouts.size());
        Workout workout = workouts.get(0);
        assertEquals(resources.getString(R.string.starter_workout_name), workout.name);
        assertEquals(new Workout().transitionSeconds, workout.transitionSeconds);
        assertEquals(new Workout().countdownEnabled, workout.countdownEnabled);
    }

    @Test
    public void starterWorkout_containsEveryStarterExerciseInOrderForTheConfiguredSeconds() {
        int seconds = resources.getInteger(R.integer.starter_exercise_seconds);
        String[] titles = resources.getStringArray(R.array.starter_exercise_titles);
        Workout workout = database.workoutDao().getAll().get(0);

        List<WorkoutExercise> entries = database.workoutExerciseDao().getByWorkout(workout.id);

        assertEquals(titles.length, entries.size());
        for (int i = 0; i < entries.size(); i++) {
            WorkoutExercise entry = entries.get(i);
            assertEquals(i, entry.position);
            assertEquals(seconds, entry.durationSeconds);
            assertEquals(titles[i], database.exerciseDao().getById(entry.exerciseId).title);
        }
    }

    @Test
    public void existingDatabase_isNotSeededAgainOnReopen() {
        int exercisesBefore = database.exerciseDao().getAll().size();
        int workoutsBefore = database.workoutDao().getAll().size();
        database.close();

        database = open();

        assertEquals(exercisesBefore, database.exerciseDao().getAll().size());
        assertEquals(workoutsBefore, database.workoutDao().getAll().size());
    }

    private AppDatabase open() {
        return Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                .addCallback(StarterSet.callback(context))
                .allowMainThreadQueries()
                .build();
    }
}
