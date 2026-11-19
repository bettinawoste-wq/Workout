package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
 * Creates a version 6 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_6_7}, lets Room validate the result against the version 7
 * schema and reads it as stored then (the entities have since moved on, see
 * {@link Migration7To8Test}).
 */
@RunWith(AndroidJUnit4.class)
public class Migration6To7Test {

    private static final String DB_NAME = "migration-6-7-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private SupportSQLiteDatabase db;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase old = helper.createDatabase(DB_NAME, 6)) {
            old.execSQL("INSERT INTO `Exercise` (`id`, `title`) VALUES (1, 'Squat')");
            old.execSQL("INSERT INTO `Workout` (`id`, `name`, `transitionSeconds`,"
                    + " `countdownEnabled`) VALUES (1, 'Push Day', 3, 1)");
            old.execSQL("INSERT INTO `Workout` (`id`, `name`, `transitionSeconds`,"
                    + " `countdownEnabled`) VALUES (2, 'Long Rest', 3, 0)");
            // A deleted workout's id, still referred to by its history.
            old.execSQL("INSERT INTO `Workout` (`id`, `name`, `transitionSeconds`,"
                    + " `countdownEnabled`) VALUES (5, 'Gone', 3, 1)");
            old.execSQL("DELETE FROM `Workout` WHERE `id` = 5");
            old.execSQL("INSERT INTO `WorkoutExercise` (`workoutId`, `exerciseId`, `position`,"
                    + " `durationSeconds`) VALUES (1, 1, 0, 45)");
        }
        db = helper.runMigrationsAndValidate(DB_NAME, 7, true, AppDatabase.MIGRATION_6_7);
    }

    @After
    public void tearDown() throws IOException {
        db.close();
    }

    @Test
    public void everyExistingWorkoutFollowsTheDefaultTransition() {
        try (Cursor cursor = db.query("SELECT `transitionSeconds` FROM `Workout`")) {
            assertEquals(2, cursor.getCount());
            while (cursor.moveToNext()) assertTrue(cursor.isNull(0));
        }
    }

    @Test
    public void existingWorkoutsKeepTheirOtherDataAndTheirExercises() {
        try (Cursor cursor = db.query(
                "SELECT `name`, `countdownEnabled` FROM `Workout` WHERE `id` = 2")) {
            cursor.moveToFirst();
            assertEquals("Long Rest", cursor.getString(0));
            assertEquals(0, cursor.getInt(1));
        }
        try (Cursor cursor = db.query(
                "SELECT `durationSeconds` FROM `WorkoutExercise` WHERE `workoutId` = 1")) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(45, cursor.getInt(0));
        }
    }

    @Test
    public void deletingAWorkoutStillDeletesItsWorkoutExercises() {
        db.execSQL("PRAGMA foreign_keys = ON");
        db.execSQL("DELETE FROM `Workout` WHERE `id` = 1");

        try (Cursor cursor = db.query("SELECT * FROM `WorkoutExercise`")) {
            assertEquals(0, cursor.getCount());
        }
    }

    @Test
    public void aNewWorkoutGetsAnIdNoWorkoutHadBefore() {
        ContentValues values = new ContentValues();
        values.put("name", "New");
        values.put("countdownEnabled", 1);

        assertTrue(db.insert("Workout", SQLiteDatabase.CONFLICT_ABORT, values) > 5);
    }
}
