package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExerciseRow;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Creates a version 6 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_6_7}, and lets Room validate the result against the version 7
 * schema before the DAOs read it.
 */
@RunWith(AndroidJUnit4.class)
public class Migration6To7Test {

    private static final String DB_NAME = "migration-6-7-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private AppDatabase database;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 6)) {
            db.execSQL("INSERT INTO `Exercise` (`id`, `title`) VALUES (1, 'Squat')");
            db.execSQL("INSERT INTO `Workout` (`id`, `name`, `transitionSeconds`, "
                    + "`countdownEnabled`) VALUES (1, 'Push Day', 3, 1)");
            db.execSQL("INSERT INTO `Workout` (`id`, `name`, `transitionSeconds`, "
                    + "`countdownEnabled`) VALUES (2, 'Long Rest', 3, 0)");
            // A deleted workout's id, still referred to by its history.
            db.execSQL("INSERT INTO `Workout` (`id`, `name`, `transitionSeconds`, "
                    + "`countdownEnabled`) VALUES (5, 'Gone', 3, 1)");
            db.execSQL("DELETE FROM `Workout` WHERE `id` = 5");
            db.execSQL("INSERT INTO `WorkoutExercise` (`workoutId`, `exerciseId`, `position`, "
                    + "`durationSeconds`) VALUES (1, 1, 0, 45)");
        }
        helper.runMigrationsAndValidate(DB_NAME, 7, true, AppDatabase.MIGRATION_6_7).close();

        database = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_6_7)
                .allowMainThreadQueries()
                .build();
        helper.closeWhenFinished(database);
    }

    @Test
    public void everyExistingWorkoutFollowsTheDefaultTransition() {
        List<Workout> workouts = database.workoutDao().getAll();

        assertEquals(2, workouts.size());
        for (Workout workout : workouts) {
            assertNull(workout.name, workout.transitionSeconds);
        }
    }

    @Test
    public void existingWorkoutsKeepTheirOtherDataAndTheirExercises() {
        Workout longRest = database.workoutDao().getById(2);
        assertEquals("Long Rest", longRest.name);
        assertFalse(longRest.countdownEnabled);

        List<WorkoutExerciseRow> rows = database.workoutExerciseDao().getByWorkout(1);
        assertEquals(1, rows.size());
        assertEquals(45, rows.get(0).durationSeconds);
    }

    @Test
    public void deletingAWorkoutStillDeletesItsWorkoutExercises() {
        database.workoutDao().delete(database.workoutDao().getById(1));

        assertTrue(database.workoutExerciseDao().getByWorkout(1).isEmpty());
    }

    @Test
    public void aNewWorkoutGetsAnIdNoWorkoutHadBefore() {
        Workout workout = new Workout();
        workout.name = "New";

        assertTrue(database.workoutDao().insert(workout) > 5);
    }
}
