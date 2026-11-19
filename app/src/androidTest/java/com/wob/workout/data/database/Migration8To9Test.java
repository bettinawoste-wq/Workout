package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;

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
 * Creates a version 8 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_8_9}, lets Room validate the result against the version 9
 * schema and reads it as stored then (the database has since moved on, see
 * {@link Migration9To10Test}).
 */
@RunWith(AndroidJUnit4.class)
public class Migration8To9Test {

    private static final String DB_NAME = "migration-8-9-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private SupportSQLiteDatabase db;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase old = helper.createDatabase(DB_NAME, 8)) {
            old.execSQL("INSERT INTO `Workout` (`id`, `name`, `countdownEnabled`) "
                    + "VALUES (1, 'Leg Day', 1)");
            old.execSQL("INSERT INTO `WorkoutSession` (`id`, `workoutId`, `workoutName`, "
                    + "`startedAt`, `finishedAt`, `activeDurationSeconds`, `completed`, "
                    + "`restSeconds`) VALUES (1, 1, 'Leg Day', 1000, 2000, 75, 1, 30)");
        }
        db = helper.runMigrationsAndValidate(DB_NAME, 9, true, AppDatabase.MIGRATION_8_9);
    }

    @After
    public void tearDown() throws IOException {
        db.close();
    }

    @Test
    public void thereIsNoWeeklyGoalAfterTheUpdate() {
        try (Cursor cursor = goalChanges()) {
            assertEquals(0, cursor.getCount());
        }
    }

    @Test
    public void aWeeklyGoalChangeCanBeRecorded() {
        db.execSQL("INSERT INTO `WeeklyGoalChange` (`weekStartDay`, `trainingDays`) "
                + "VALUES (20408, 3)");

        try (Cursor cursor = goalChanges()) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(20_408, cursor.getLong(0));
            assertEquals(3, cursor.getInt(1));
        }
    }

    @Test
    public void theHistoryIsUntouched() {
        try (Cursor cursor = db.query("SELECT `activeDurationSeconds`, `completed`, "
                + "`restSeconds` FROM `WorkoutSession` WHERE `id` = 1")) {
            cursor.moveToFirst();
            assertEquals(75, cursor.getInt(0));
            assertEquals(1, cursor.getInt(1));
            assertEquals(30, cursor.getInt(2));
        }
    }

    private Cursor goalChanges() {
        return db.query("SELECT `weekStartDay`, `trainingDays` FROM `WeeklyGoalChange`");
    }
}
