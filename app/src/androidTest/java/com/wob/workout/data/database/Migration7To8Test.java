package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.data.entity.WorkoutExerciseRow;
import com.wob.workout.data.entity.WorkoutSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Creates a version 7 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_7_8}, and lets Room validate the result against the version 8
 * schema before the DAOs read it.
 */
@RunWith(AndroidJUnit4.class)
public class Migration7To8Test {

    private static final String DB_NAME = "migration-7-8-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private AppDatabase database;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 7)) {
            db.execSQL("INSERT INTO `Exercise` (`id`, `title`) VALUES (1, 'Squat')");
            db.execSQL("INSERT INTO `Exercise` (`id`, `title`) VALUES (2, 'Lunge')");
            db.execSQL("INSERT INTO `Workout` (`id`, `name`, `countdownEnabled`) "
                    + "VALUES (1, 'Leg Day', 1)");
            db.execSQL("INSERT INTO `WorkoutExercise` (`id`, `workoutId`, `exerciseId`, "
                    + "`position`, `durationSeconds`) VALUES (10, 1, 2, 1, 30)");
            db.execSQL("INSERT INTO `WorkoutExercise` (`id`, `workoutId`, `exerciseId`, "
                    + "`position`, `durationSeconds`) VALUES (11, 1, 1, 0, 45)");
            db.execSQL("INSERT INTO `WorkoutSession` (`id`, `workoutId`, `workoutName`, "
                    + "`startedAt`, `finishedAt`, `activeDurationSeconds`, `completed`) "
                    + "VALUES (1, 1, 'Leg Day', 1000, 2000, 75, 1)");
        }
        helper.runMigrationsAndValidate(DB_NAME, 8, true, AppDatabase.MIGRATION_7_8).close();

        database = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_7_8)
                .allowMainThreadQueries()
                .build();
        helper.closeWhenFinished(database);
    }

    @Test
    public void existingPlacesKeepTheirExerciseIdAndOrder() {
        List<WorkoutExerciseRow> rows = database.workoutExerciseDao().getByWorkout(1);

        assertEquals(2, rows.size());
        assertEquals(11, rows.get(0).id);
        assertEquals(Long.valueOf(1), rows.get(0).exerciseId);
        assertEquals(45, rows.get(0).durationSeconds);
        assertEquals(10, rows.get(1).id);
        assertEquals(Long.valueOf(2), rows.get(1).exerciseId);
    }

    @Test
    public void aPlaceCanNowBeARestWithoutAnExercise() {
        WorkoutExerciseRow rest = new WorkoutExerciseRow();
        rest.workoutId = 1;
        rest.exerciseId = null;
        rest.position = 2;
        rest.durationSeconds = 30;
        database.workoutExerciseDao().insert(rest);

        List<WorkoutExerciseRow> rows = database.workoutExerciseDao().getByWorkout(1);
        assertEquals(3, rows.size());
        assertNull(rows.get(2).exerciseId);
        assertEquals(30, rows.get(2).durationSeconds);
    }

    @Test
    public void deletingAnExerciseStillDeletesItsPlacesButNotTheRests() {
        WorkoutExerciseRow rest = new WorkoutExerciseRow();
        rest.workoutId = 1;
        rest.position = 2;
        rest.durationSeconds = 30;
        database.workoutExerciseDao().insert(rest);

        database.exerciseDao().delete(database.exerciseDao().getById(1));

        List<WorkoutExerciseRow> rows = database.workoutExerciseDao().getByWorkout(1);
        assertEquals(2, rows.size());
        assertEquals(Long.valueOf(2), rows.get(0).exerciseId);
        assertNull(rows.get(1).exerciseId);
    }

    @Test
    public void existingWorkoutSessionsHaveNoRestTime() {
        WorkoutSession session = database.workoutSessionDao().getById(1);

        assertEquals(75, session.activeDurationSeconds);
        assertTrue(session.completed);
        assertNull(session.restSeconds);
    }
}
