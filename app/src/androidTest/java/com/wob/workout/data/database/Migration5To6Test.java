package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.data.entity.Workout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Creates a version 5 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_5_6}, and lets Room validate the result against the version 6
 * schema before the DAO reads it.
 */
@RunWith(AndroidJUnit4.class)
public class Migration5To6Test {

    private static final String DB_NAME = "migration-5-6-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private AppDatabase database;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 5)) {
            db.execSQL("INSERT INTO `Workout` (`name`, `transitionSeconds`, `countdownEnabled`) "
                    + "VALUES ('Push Day', 5, 1)");
            db.execSQL("INSERT INTO `Workout` (`name`, `transitionSeconds`, `countdownEnabled`) "
                    + "VALUES ('Long Rest', 10, 0)");
        }
        helper.runMigrationsAndValidate(DB_NAME, 6, true, AppDatabase.MIGRATION_5_6).close();

        database = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_5_6)
                .allowMainThreadQueries()
                .build();
        helper.closeWhenFinished(database);
    }

    @Test
    public void everyExistingWorkoutsTransitionLastsThreeSeconds() {
        List<Workout> workouts = database.workoutDao().getAll();

        assertEquals(2, workouts.size());
        for (Workout workout : workouts) {
            assertEquals(workout.name, 3, workout.transitionSeconds);
        }
    }

    @Test
    public void existingWorkoutsKeepTheirOtherData() {
        Workout longRest = database.workoutDao().getAll().get(0); // newest first

        assertEquals("Long Rest", longRest.name);
        assertFalse(longRest.countdownEnabled);
    }
}
