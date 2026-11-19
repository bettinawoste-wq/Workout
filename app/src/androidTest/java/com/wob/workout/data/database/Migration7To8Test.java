package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Creates a version 7 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_7_8}, lets Room validate the result against the version 8
 * schema and reads it as stored then (the database has since moved on, see
 * {@link Migration8To9Test}).
 */
@RunWith(AndroidJUnit4.class)
public class Migration7To8Test {

    private static final String DB_NAME = "migration-7-8-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private SupportSQLiteDatabase db;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase old = helper.createDatabase(DB_NAME, 7)) {
            old.execSQL("INSERT INTO `Exercise` (`id`, `title`) VALUES (1, 'Squat')");
            old.execSQL("INSERT INTO `Exercise` (`id`, `title`) VALUES (2, 'Lunge')");
            old.execSQL("INSERT INTO `Workout` (`id`, `name`, `countdownEnabled`) "
                    + "VALUES (1, 'Leg Day', 1)");
            old.execSQL("INSERT INTO `WorkoutExercise` (`id`, `workoutId`, `exerciseId`, "
                    + "`position`, `durationSeconds`) VALUES (10, 1, 2, 1, 30)");
            old.execSQL("INSERT INTO `WorkoutExercise` (`id`, `workoutId`, `exerciseId`, "
                    + "`position`, `durationSeconds`) VALUES (11, 1, 1, 0, 45)");
            old.execSQL("INSERT INTO `WorkoutSession` (`id`, `workoutId`, `workoutName`, "
                    + "`startedAt`, `finishedAt`, `activeDurationSeconds`, `completed`) "
                    + "VALUES (1, 1, 'Leg Day', 1000, 2000, 75, 1)");
        }
        db = helper.runMigrationsAndValidate(DB_NAME, 8, true, AppDatabase.MIGRATION_7_8);
    }

    @After
    public void tearDown() throws IOException {
        db.close();
    }

    @Test
    public void existingPlacesKeepTheirExerciseIdAndOrder() {
        try (Cursor cursor = places()) {
            assertEquals(2, cursor.getCount());
            cursor.moveToNext();
            assertEquals(11, cursor.getLong(0));
            assertEquals(1, cursor.getLong(1));
            assertEquals(45, cursor.getInt(2));
            cursor.moveToNext();
            assertEquals(10, cursor.getLong(0));
            assertEquals(2, cursor.getLong(1));
        }
    }

    @Test
    public void aPlaceCanNowBeARestWithoutAnExercise() {
        insertRest();

        try (Cursor cursor = places()) {
            assertEquals(3, cursor.getCount());
            cursor.moveToLast();
            assertTrue(cursor.isNull(1));
            assertEquals(30, cursor.getInt(2));
        }
    }

    @Test
    public void deletingAnExerciseStillDeletesItsPlacesButNotTheRests() {
        insertRest();
        // Room switches foreign keys on for every connection it opens.
        db.execSQL("PRAGMA foreign_keys = ON");

        db.execSQL("DELETE FROM `Exercise` WHERE `id` = 1");

        try (Cursor cursor = places()) {
            assertEquals(2, cursor.getCount());
            cursor.moveToNext();
            assertEquals(2, cursor.getLong(1));
            cursor.moveToNext();
            assertTrue(cursor.isNull(1));
        }
    }

    @Test
    public void existingWorkoutSessionsHaveNoRestTime() {
        try (Cursor cursor = db.query("SELECT `activeDurationSeconds`, `completed`, "
                + "`restSeconds` FROM `WorkoutSession` WHERE `id` = 1")) {
            cursor.moveToFirst();
            assertEquals(75, cursor.getInt(0));
            assertEquals(1, cursor.getInt(1));
            assertTrue(cursor.isNull(2));
        }
    }

    private void insertRest() {
        db.execSQL("INSERT INTO `WorkoutExercise` (`workoutId`, `exerciseId`, `position`, "
                + "`durationSeconds`) VALUES (1, NULL, 2, 30)");
    }

    /** The places of the workout, in order: id, exercise id, duration. */
    private Cursor places() {
        return db.query("SELECT `id`, `exerciseId`, `durationSeconds` FROM `WorkoutExercise` "
                + "WHERE `workoutId` = 1 ORDER BY `position`");
    }
}
