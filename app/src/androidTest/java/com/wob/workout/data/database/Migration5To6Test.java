package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Creates a version 5 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_5_6}, lets Room validate the result against the version 6
 * schema and reads it as stored then (the entities have since moved on, see
 * {@link Migration6To7Test}).
 */
@RunWith(AndroidJUnit4.class)
public class Migration5To6Test {

    private static final String DB_NAME = "migration-5-6-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    @Test
    public void everyExistingWorkoutsTransitionLastsThreeSecondsAndKeepsItsOtherData()
            throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 5)) {
            db.execSQL("INSERT INTO `Workout` (`name`, `transitionSeconds`, `countdownEnabled`) "
                    + "VALUES ('Push Day', 5, 1)");
            db.execSQL("INSERT INTO `Workout` (`name`, `transitionSeconds`, `countdownEnabled`) "
                    + "VALUES ('Long Rest', 10, 0)");
        }

        try (SupportSQLiteDatabase db =
                     helper.runMigrationsAndValidate(DB_NAME, 6, true, AppDatabase.MIGRATION_5_6);
             Cursor cursor = db.query("SELECT `name`, `transitionSeconds`, `countdownEnabled`"
                     + " FROM `Workout` ORDER BY `id`")) {
            assertEquals(2, cursor.getCount());
            cursor.moveToNext();
            assertEquals("Push Day", cursor.getString(0));
            assertEquals(3, cursor.getInt(1));
            assertEquals(1, cursor.getInt(2));
            cursor.moveToNext();
            assertEquals("Long Rest", cursor.getString(0));
            assertEquals(3, cursor.getInt(1));
            assertEquals(0, cursor.getInt(2));
        }
    }
}
